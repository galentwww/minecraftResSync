package com.minecraft.sync;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.function.BiConsumer;

public class FileDownloader {
    
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    
    // Base download directory (same directory as JAR)
    private static final String BASE_DIR = System.getProperty("user.dir");
    
    /**
     * Download progress callback interface
     * Parameters: (bytesRead, totalBytes)
     */
    public interface ProgressCallback {
        void onProgress(long bytesRead, long totalBytes);
    }
    
    /**
     * Download a file from the given URL to the appropriate directory based on catalog
     * Uses hash-based verification for update detection and file management
     * @param modInfo The mod information containing download URL, catalog, and hash
     * @param progressCallback Callback to report download progress
     * @return true if download successful, false otherwise
     */
    public static boolean downloadFile(ModInfo modInfo, ProgressCallback progressCallback) {
        if (modInfo.getRes() == null || modInfo.getRes().trim().isEmpty()) {
            System.err.println("No download URL found for: " + modInfo.getFriendlyName());
            return false;
        }
        
        try {
            // Create target directory structure
            Path targetDir = createDirectoryStructure(modInfo.getCatelog());
            if (targetDir == null) {
                System.err.println("Failed to create directory for catalog: " + modInfo.getCatelog());
                return false;
            }
            
            // Determine target file name using friendly_name with proper extension
            String targetFileName = getTargetFileName(modInfo);
            
            Path targetFile = targetDir.resolve(targetFileName);
            String expectedHash = modInfo.getHash();
            
            // Hash-based file verification and management
            FileStatus status = checkFileStatus(targetDir, targetFileName, modInfo.getFriendlyName(), expectedHash);
            
            switch (status.type) {
                case FILE_UP_TO_DATE:
                    System.out.println("File is up-to-date: " + targetFileName);
                    if (progressCallback != null) {
                        progressCallback.onProgress(100, 100);
                    }
                    return true;
                    
                case FILE_NEEDS_UPDATE:
                    System.out.println("File exists but hash mismatch, updating: " + targetFileName);
                    // Delete old file before downloading new version
                    try {
                        Files.delete(status.existingFile);
                        System.out.println("Deleted outdated file: " + status.existingFile.getFileName());
                    } catch (Exception e) {
                        System.err.println("Failed to delete old file: " + e.getMessage());
                    }
                    break;
                    
                case FILE_NEEDS_RENAME:
                    System.out.println("Found file with matching hash but different name, renaming...");
                    try {
                        Path newName = targetDir.resolve(targetFileName);
                        Files.move(status.existingFile, newName);
                        System.out.println("Renamed " + status.existingFile.getFileName() + " to " + newName.getFileName());
                        
                        if (progressCallback != null) {
                            progressCallback.onProgress(100, 100);
                        }
                        return true;
                    } catch (Exception e) {
                        System.err.println("Failed to rename file: " + e.getMessage());
                        // Continue with download if rename fails
                    }
                    break;
                    
                case FILE_NOT_FOUND:
                default:
                    System.out.println("File not found, downloading: " + targetFileName);
                    break;
            }
            
            // Download the file
            return downloadFileFromUrl(modInfo.getRes(), targetFile, progressCallback, expectedHash);
            
        } catch (Exception e) {
            System.err.println("Error downloading " + modInfo.getFriendlyName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Generate target file name using friendly_name with appropriate extension
     */
    private static String getTargetFileName(ModInfo modInfo) {
        String friendlyName = modInfo.getFriendlyName();
        if (friendlyName == null || friendlyName.trim().isEmpty()) {
            // Fallback to raw name if friendly name is not available
            String rawName = modInfo.getRawName();
            if (rawName != null && !rawName.trim().isEmpty()) {
                return rawName;
            }
            // Last resort: extract from URL
            return extractFileNameFromUrl(modInfo.getRes());
        }
        
        // Determine extension from raw_name or URL
        String extension = getFileExtension(modInfo);
        
        // Combine friendly name with extension
        return friendlyName + extension;
    }
    
    /**
     * Get file extension from raw_name or URL
     */
    private static String getFileExtension(ModInfo modInfo) {
        // First try to get extension from raw_name
        String rawName = modInfo.getRawName();
        if (rawName != null && !rawName.trim().isEmpty()) {
            int dotIndex = rawName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < rawName.length() - 1) {
                return rawName.substring(dotIndex);
            }
        }
        
        // Fallback to URL
        String url = modInfo.getRes();
        if (url != null) {
            try {
                String path = new URL(url).getPath();
                int dotIndex = path.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < path.length() - 1) {
                    return path.substring(dotIndex);
                }
            } catch (Exception e) {
                // Ignore URL parsing errors
            }
        }
        
        // Default extension based on catalog
        String catalog = modInfo.getCatelog();
        if (catalog != null) {
            switch (catalog.toLowerCase()) {
                case "mods":
                    return ".jar";
                case "resourcepacks":
                case "shaderpacks":
                    return ".zip";
                case "config":
                    return ".json";
            }
        }
        
        // Final fallback
        return ".jar";
    }
    
    /**
     * Create the appropriate directory structure based on catalog (public for UI access)
     */
    public static Path createDirectoryStructure(String catalog) {
        try {
            Path baseDir = Paths.get(BASE_DIR);
            Path targetDir;
            
            switch (catalog.toLowerCase()) {
                case "mods":
                    targetDir = baseDir.resolve("mods");
                    break;
                case "resourcepacks":
                    targetDir = baseDir.resolve("resourcepacks");
                    break;
                case "shaderpacks":
                    targetDir = baseDir.resolve("shaderpacks");
                    break;
                case "config":
                    targetDir = baseDir.resolve("config");
                    break;
                default:
                    // For unknown catalogs, create a directory with the catalog name
                    targetDir = baseDir.resolve(catalog.toLowerCase());
                    break;
            }
            
            // Create directory if it doesn't exist
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
                System.out.println("Created directory: " + targetDir.toString());
            }
            
            return targetDir;
            
        } catch (Exception e) {
            System.err.println("Failed to create directory structure: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract filename from URL
     */
    private static String extractFileNameFromUrl(String urlStr) {
        try {
            String path = new URL(urlStr).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            return fileName.isEmpty() ? "downloaded_file" : fileName;
        } catch (Exception e) {
            return "downloaded_file_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Download file from URL with progress tracking and hash verification
     */
    private static boolean downloadFileFromUrl(String urlStr, Path targetFile, ProgressCallback progressCallback, String expectedHash) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            
            // Set timeouts and headers
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "MinecraftResSyncTool/1.0");
            connection.setRequestProperty("Accept", "*/*");
            
            // Connect and check response
            connection.connect();
            int responseCode = connection.getResponseCode();
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("HTTP error " + responseCode + " for URL: " + urlStr);
                return false;
            }
            
            // Get content length for progress tracking
            long contentLength = connection.getContentLengthLong();
            
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(targetFile.toFile());
            
            byte[] buffer = new byte[BUFFER_SIZE];
            long totalBytesRead = 0;
            int bytesRead;
            
            System.out.println("Downloading: " + targetFile.getFileName() + " (" + formatBytes(contentLength) + ")");
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                
                // Report progress
                if (progressCallback != null) {
                    progressCallback.onProgress(totalBytesRead, contentLength);
                }
            }
            
            System.out.println("Downloaded successfully: " + targetFile.getFileName());
            
            // Verify hash if provided
            if (expectedHash != null && !expectedHash.trim().isEmpty()) {
                String actualHash = calculateFileHash(targetFile);
                if (actualHash != null && actualHash.equalsIgnoreCase(expectedHash)) {
                    System.out.println("Hash verification successful: " + actualHash);
                    return true;
                } else {
                    System.err.println("Hash verification failed!");
                    System.err.println("Expected: " + expectedHash);
                    System.err.println("Actual: " + actualHash);
                    
                    // Delete corrupted file
                    try {
                        Files.delete(targetFile);
                        System.out.println("Deleted corrupted file: " + targetFile.getFileName());
                    } catch (Exception deleteEx) {
                        System.err.println("Failed to delete corrupted file: " + deleteEx.getMessage());
                    }
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Download failed for " + urlStr + ": " + e.getMessage());
            
            // Clean up partial download
            try {
                if (Files.exists(targetFile)) {
                    Files.delete(targetFile);
                }
            } catch (Exception deleteEx) {
                System.err.println("Failed to clean up partial download: " + deleteEx.getMessage());
            }
            
            return false;
            
        } finally {
            // Clean up resources
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    /**
     * Format bytes to human-readable string
     */
    private static String formatBytes(long bytes) {
        if (bytes < 0) return "Unknown size";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * Get the total directory size for a catalog
     */
    public static long getDirectorySize(String catalog) {
        try {
            Path dir = createDirectoryStructure(catalog);
            if (dir == null || !Files.exists(dir)) {
                return 0;
            }
            
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();
                    
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Check if a file is ready (exists with correct name and hash)
     * This is different from fileExists() which returns true for files that need renaming
     */
    public static boolean isFileReady(ModInfo modInfo) {
        try {
            Path targetDir = createDirectoryStructure(modInfo.getCatelog());
            if (targetDir == null) return false;
            
            String fileName = getTargetFileName(modInfo);
            
            FileStatus status = checkFileStatus(targetDir, fileName, modInfo.getFriendlyName(), modInfo.getHash());
            return status.type == FileStatusType.FILE_UP_TO_DATE;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a file exists in the appropriate directory (hash-aware)
     */
    public static boolean fileExists(ModInfo modInfo) {
        try {
            Path targetDir = createDirectoryStructure(modInfo.getCatelog());
            if (targetDir == null) return false;
            
            String fileName = getTargetFileName(modInfo);
            
            FileStatus status = checkFileStatus(targetDir, fileName, modInfo.getFriendlyName(), modInfo.getHash());
            return status.type != FileStatusType.FILE_NOT_FOUND;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * File status types for hash-based verification
     */
    private enum FileStatusType {
        FILE_NOT_FOUND,        // No file exists
        FILE_UP_TO_DATE,       // File exists with correct hash
        FILE_NEEDS_UPDATE,     // File exists but hash mismatch (needs update)
        FILE_NEEDS_RENAME      // Different file exists with matching hash (needs rename)
    }
    
    /**
     * File status information
     */
    private static class FileStatus {
        public final FileStatusType type;
        public final Path existingFile;
        
        public FileStatus(FileStatusType type, Path existingFile) {
            this.type = type;
            this.existingFile = existingFile;
        }
    }
    
    /**
     * Check file status based on filename and hash verification
     */
    private static FileStatus checkFileStatus(Path targetDir, String targetFileName, String friendlyName, String expectedHash) {
        try {
            if (expectedHash == null || expectedHash.trim().isEmpty()) {
                // No hash provided, use simple file existence check
                Path targetFile = targetDir.resolve(targetFileName);
                if (Files.exists(targetFile)) {
                    return new FileStatus(FileStatusType.FILE_UP_TO_DATE, targetFile);
                }
                return new FileStatus(FileStatusType.FILE_NOT_FOUND, null);
            }
            
            // Check if target file exists and has correct hash
            Path targetFile = targetDir.resolve(targetFileName);
            if (Files.exists(targetFile)) {
                String actualHash = calculateFileHash(targetFile);
                if (expectedHash.equalsIgnoreCase(actualHash)) {
                    return new FileStatus(FileStatusType.FILE_UP_TO_DATE, targetFile);
                } else {
                    return new FileStatus(FileStatusType.FILE_NEEDS_UPDATE, targetFile);
                }
            }
            
            // Check if any other file in the directory has the matching hash
            if (Files.exists(targetDir) && Files.isDirectory(targetDir)) {
                try {
                    Path matchingFile = Files.list(targetDir)
                        .filter(Files::isRegularFile)
                        .filter(file -> {
                            try {
                                String fileHash = calculateFileHash(file);
                                return expectedHash.equalsIgnoreCase(fileHash);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .findFirst()
                        .orElse(null);
                        
                    if (matchingFile != null) {
                        return new FileStatus(FileStatusType.FILE_NEEDS_RENAME, matchingFile);
                    }
                } catch (Exception e) {
                    System.err.println("Error scanning directory for hash matches: " + e.getMessage());
                }
            }
            
            return new FileStatus(FileStatusType.FILE_NOT_FOUND, null);
            
        } catch (Exception e) {
            System.err.println("Error checking file status: " + e.getMessage());
            return new FileStatus(FileStatusType.FILE_NOT_FOUND, null);
        }
    }
    
    /**
     * Calculate MD5 hash of a file
     */
    private static String calculateFileHash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            
            try (FileInputStream fis = new FileInputStream(file.toFile());
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = bis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            System.err.println("Error calculating file hash: " + e.getMessage());
            return null;
        }
    }
}
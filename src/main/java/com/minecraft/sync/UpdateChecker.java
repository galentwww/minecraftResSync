package com.minecraft.sync;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UpdateChecker {
    private static final String UPDATE_API_URL = "https://api.galentwww.cn/items/mod_essential?filter[friendly_name][_eq]=MinecraftResSync";
    private static final String CURRENT_JAR_NAME = "minecraftResSync.jar";
    
    public static void checkAndUpdate() {
        try {
            System.out.println("检查更新中...");
            
            String currentJarPath = getCurrentJarPath();
            if (currentJarPath == null) {
                System.out.println("无法确定当前JAR文件路径，跳过更新检查");
                return;
            }
            
            String currentHash = calculateFileHash(currentJarPath);
            if (currentHash == null) {
                System.out.println("无法计算当前文件哈希，跳过更新检查");
                return;
            }
            
            System.out.println("当前文件哈希: " + currentHash);
            
            String apiResponse = HttpClient.get(UPDATE_API_URL);
            if (apiResponse == null) {
                System.out.println("无法获取更新信息，跳过更新检查");
                return;
            }
            
            Gson gson = new Gson();
            JsonObject response = gson.fromJson(apiResponse, JsonObject.class);
            JsonArray data = response.getAsJsonArray("data");
            
            if (data == null || data.size() == 0) {
                System.out.println("未找到更新信息");
                return;
            }
            
            JsonObject updateInfo = data.get(0).getAsJsonObject();
            String latestHash = updateInfo.get("hash").getAsString();
            String downloadUrl = updateInfo.get("res").getAsString();
            
            System.out.println("最新文件哈希: " + latestHash);
            
            if (!currentHash.equals(latestHash)) {
                System.out.println("发现新版本！");
                
                if (isGUIMode()) {
                    int result = JOptionPane.showConfirmDialog(
                        null,
                        "发现新版本，是否立即更新？\n\n当前版本: " + currentHash.substring(0, 8) + 
                        "\n最新版本: " + latestHash.substring(0, 8),
                        "更新提醒",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    );
                    
                    if (result == JOptionPane.YES_OPTION) {
                        downloadUpdate(downloadUrl, currentJarPath);
                    }
                } else {
                    System.out.println("命令行模式下自动更新");
                    downloadUpdate(downloadUrl, currentJarPath);
                }
            } else {
                System.out.println("当前已是最新版本");
            }
            
        } catch (Exception e) {
            System.err.println("更新检查失败: " + e.getMessage());
        }
    }
    
    private static String getCurrentJarPath() {
        try {
            String jarPath = UpdateChecker.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            
            if (jarPath.endsWith(".jar")) {
                return jarPath;
            }
            
            Path targetDir = Paths.get("target");
            if (Files.exists(targetDir)) {
                Path jarFile = targetDir.resolve(CURRENT_JAR_NAME);
                if (Files.exists(jarFile)) {
                    return jarFile.toString();
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String calculateFileHash(String filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(filePath);
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            fis.close();
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            return null;
        }
    }
    
    private static void downloadUpdate(String downloadUrl, String currentJarPath) {
        try {
            System.out.println("开始下载更新: " + downloadUrl);
            
            String backupPath = currentJarPath + ".backup";
            Files.copy(Paths.get(currentJarPath), Paths.get(backupPath));
            System.out.println("已创建备份文件: " + backupPath);
            
            URL url = new URL(downloadUrl);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(currentJarPath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
            
            String newHash = calculateFileHash(currentJarPath);
            System.out.println("更新完成！新文件哈希: " + newHash);
            
            if (isGUIMode()) {
                JOptionPane.showMessageDialog(
                    null,
                    "更新完成！请重新启动应用程序。\n\n新版本哈希: " + 
                    (newHash != null ? newHash.substring(0, 8) : "未知"),
                    "更新完成",
                    JOptionPane.INFORMATION_MESSAGE
                );
                System.exit(0);
            } else {
                System.out.println("更新完成！请重新启动应用程序。");
                System.exit(0);
            }
            
        } catch (Exception e) {
            System.err.println("更新下载失败: " + e.getMessage());
            
            if (isGUIMode()) {
                JOptionPane.showMessageDialog(
                    null,
                    "更新失败: " + e.getMessage(),
                    "更新错误",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    private static boolean isGUIMode() {
        return !GraphicsEnvironment.isHeadless() && 
               System.getProperty("java.awt.headless") == null;
    }
}
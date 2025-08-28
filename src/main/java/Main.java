import com.minecraft.sync.*;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    // Platform-dependent fallback path for modlist.json
    private static String getLocalModlistPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows fallback - current directory 
            String currentDir = System.getProperty("user.dir");
            return currentDir + "\\modlist.json";
        } else {
            // macOS/Linux fallback
            return "/Users/galentwww/IdeaProjects/minecraftResSync/modlist.json";
        }
    }

    public static void main(String[] args) {
        // Check for updates before launching any mode
        UpdateChecker.checkAndUpdate();
        
        // Check if should launch GUI (no arguments or --gui flag)
        if (args.length == 0 || (args.length == 1 && "--gui".equals(args[0]))) {
            // Launch GUI without auto-fetch
            javax.swing.SwingUtilities.invokeLater(() -> {
                new MinecraftResSyncGUI().setVisible(true);
            });
            return;
        }
        
        // Check if it's a GUI launch with API URL parameter
        if (args.length == 1 && !args[0].startsWith("-")) {
            // Could be either GUI with URL or CLI mode
            String apiUrl = args[0];
            
            // Launch GUI with auto-fetch
            javax.swing.SwingUtilities.invokeLater(() -> {
                new MinecraftResSyncGUI(apiUrl).setVisible(true);
            });
            return;
        }
        
        // Check for explicit CLI mode
        if (args.length == 2 && "--cli".equals(args[0])) {
            runCliMode(args[1]);
            return;
        }
        
        // Default: treat single argument as CLI mode for backward compatibility
        if (args.length == 1) {
            runCliMode(args[0]);
            return;
        }
        
        // Invalid arguments
        System.err.println("Usage: java -jar minecraftResSync.jar [options] [api-endpoint-url]");
        System.err.println("Options:");
        System.err.println("  (no args)          Launch GUI");
        System.err.println("  --gui              Launch GUI");
        System.err.println("  --cli <url>        Run in CLI mode");
        System.err.println("  <url>              Launch GUI with auto-fetch");
        System.exit(1);
    }
    
    private static void runCliMode(String apiUrl) {
        // Continue with CLI mode
        System.out.println("=== Minecraft Resource Sync Tool ===");
        System.out.println("Starting resource synchronization...\n");

        // Parse command line arguments
        ArgumentParser parser = new ArgumentParser(new String[]{apiUrl});
        if (!parser.isValid()) {
            System.err.println("Error: " + parser.getErrorMessage());
            System.exit(1);
        }

        String apiEndpoint = parser.getApiEndpoint();
        System.out.println("API Endpoint: " + apiEndpoint);

        try {
            // First, try to fetch from remote API
            System.out.println("\n--- Fetching from remote API ---");
            System.out.println("Connecting to: " + apiEndpoint);
            
            String jsonResponse = HttpClient.get(apiEndpoint);
            ModListResponse remoteResponse = JsonParser.parseModListFromString(jsonResponse);
            
            if (remoteResponse != null && remoteResponse.getData() != null) {
                System.out.println("Successfully fetched mod list from remote API");
                displayModList(remoteResponse);
            } else {
                System.err.println("Failed to parse remote API response");
                
                // Fallback to local file
                System.out.println("\n--- Fallback to local modlist.json ---");
                ModListResponse localResponse = JsonParser.parseModListFromFile(getLocalModlistPath());
                
                if (localResponse != null && localResponse.getData() != null) {
                    System.out.println("Using local modlist.json as fallback");
                    displayModList(localResponse);
                } else {
                    System.err.println("Failed to read both remote API and local modlist.json");
                    System.exit(1);
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n=== Resource sync completed ===");
    }

    private static void displayModList(ModListResponse response) {
        if (response == null || response.getData() == null) {
            System.out.println("No mod data available");
            return;
        }

        System.out.println("Found " + response.getData().size() + " mods:");
        System.out.println("=".repeat(80));

        int count = 1;
        for (ModInfo mod : response.getData()) {
            System.out.printf("%3d. %s\n", count++, mod.toString());
        }

        System.out.println("=".repeat(80));
        
        // Display summary by subject
        displaySummaryBySubject(response);
    }

    private static void displaySummaryBySubject(ModListResponse response) {
        System.out.println("\n--- Summary by Subject ---");
        
        java.util.Map<String, Long> subjectCount = response.getData().stream()
            .filter(mod -> mod.getSubject() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                ModInfo::getSubject, 
                java.util.stream.Collectors.counting()
            ));

        subjectCount.entrySet().stream()
            .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(entry -> 
                System.out.printf("%-15s: %d mods\n", entry.getKey(), entry.getValue())
            );

        long requiredCount = response.getData().stream()
            .filter(ModInfo::isRequired)
            .count();

        System.out.printf("\nRequired mods: %d/%d\n", 
            requiredCount, response.getData().size());
    }
}
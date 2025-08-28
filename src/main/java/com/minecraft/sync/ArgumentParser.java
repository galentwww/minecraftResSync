package com.minecraft.sync;

public class ArgumentParser {
    private String apiEndpoint;
    private boolean isValid;
    private String errorMessage;

    public ArgumentParser(String[] args) {
        parseArguments(args);
    }

    private void parseArguments(String[] args) {
        if (args == null || args.length == 0) {
            this.isValid = false;
            this.errorMessage = "Usage: java -jar minecraftResSync.jar <api-endpoint-url>";
            return;
        }

        if (args.length > 1) {
            this.isValid = false;
            this.errorMessage = "Too many arguments. Usage: java -jar minecraftResSync.jar <api-endpoint-url>";
            return;
        }

        this.apiEndpoint = args[0];
        
        // Basic URL validation
        if (!isValidUrl(apiEndpoint)) {
            this.isValid = false;
            this.errorMessage = "Invalid URL format: " + apiEndpoint;
            return;
        }

        this.isValid = true;
    }

    private boolean isValidUrl(String url) {
        return url != null && 
               (url.startsWith("http://") || url.startsWith("https://")) &&
               url.length() > 10; // Basic length check
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
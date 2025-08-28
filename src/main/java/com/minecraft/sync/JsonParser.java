package com.minecraft.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class JsonParser {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static ModListResponse parseModList(String jsonContent) {
        return parseModListFromString(jsonContent);
    }
    
    public static ModListResponse parseModListFromString(String jsonContent) {
        try {
            return gson.fromJson(jsonContent, ModListResponse.class);
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            return null;
        }
    }

    public static ModListResponse parseModListFromFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Reader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, ModListResponse.class);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return null;
        }
    }

    public static String toJson(ModListResponse response) {
        return gson.toJson(response);
    }
}
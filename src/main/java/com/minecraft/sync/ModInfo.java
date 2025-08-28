package com.minecraft.sync;

public class ModInfo {
    private String catelog;
    private String description;
    private String friendly_name;
    private String hash;
    private int id;
    private boolean is_require;
    private String raw_name;
    private String res;
    private String subject;

    // Getters and setters
    public String getCatelog() {
        return catelog;
    }

    public void setCatelog(String catelog) {
        this.catelog = catelog;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFriendlyName() {
        return friendly_name;
    }

    public void setFriendlyName(String friendly_name) {
        this.friendly_name = friendly_name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isRequired() {
        return is_require;
    }

    public void setRequired(boolean is_require) {
        this.is_require = is_require;
    }

    public String getRawName() {
        return raw_name;
    }

    public void setRawName(String raw_name) {
        this.raw_name = raw_name;
    }

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String toString() {
        return String.format("Mod: %s (ID: %d) - %s [%s] - %s", 
            friendly_name, id, subject, is_require ? "Required" : "Optional", raw_name);
    }
}
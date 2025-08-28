package com.minecraft.sync;

import java.util.List;

public class ModListResponse {
    private List<ModInfo> data;

    public List<ModInfo> getData() {
        return data;
    }

    public void setData(List<ModInfo> data) {
        this.data = data;
    }
}
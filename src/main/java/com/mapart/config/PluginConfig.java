package com.mapart.config;

import com.mapart.MapArtPlugin;

import java.io.File;

public class PluginConfig {

    private final MapArtPlugin plugin;
    
    private int maxImageWidth;
    private int maxImageHeight;
    private int mapSize;
    private boolean asyncProcessing;
    private int maxConcurrentTasks;
    private File imageDirectory;

    public PluginConfig(MapArtPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.getConfig().addDefault("max-image-width", 2048);
        plugin.getConfig().addDefault("max-image-height", 2048);
        plugin.getConfig().addDefault("map-size", 128);
        plugin.getConfig().addDefault("async-processing", true);
        plugin.getConfig().addDefault("max-concurrent-tasks", 4);
        
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        this.maxImageWidth = plugin.getConfig().getInt("max-image-width", 2048);
        this.maxImageHeight = plugin.getConfig().getInt("max-image-height", 2048);
        this.mapSize = plugin.getConfig().getInt("map-size", 128);
        this.asyncProcessing = plugin.getConfig().getBoolean("async-processing", true);
        this.maxConcurrentTasks = plugin.getConfig().getInt("max-concurrent-tasks", 4);
        this.imageDirectory = new File(plugin.getDataFolder(), "images");
        
        if (!imageDirectory.exists()) {
            imageDirectory.mkdirs();
        }
    }

    public int getMaxImageWidth() {
        return maxImageWidth;
    }

    public int getMaxImageHeight() {
        return maxImageHeight;
    }

    public int getMapSize() {
        return mapSize;
    }

    public boolean isAsyncProcessing() {
        return asyncProcessing;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public File getImageDirectory() {
        return imageDirectory;
    }
}

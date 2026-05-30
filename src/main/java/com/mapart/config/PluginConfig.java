package com.mapart.config;

import com.mapart.MapArtPlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class PluginConfig {

    private final MapArtPlugin plugin;
    
    private int maxImageWidth;
    private int maxImageHeight;
    private int mapSize;
    private boolean asyncProcessing;
    private int maxConcurrentTasks;
    private File imageDirectory;
    private boolean webServerEnabled;
    private String webServerHost;
    private int webServerPort;
    private String webPublicUrl;

    public PluginConfig(MapArtPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.getConfig().addDefault("max-image-width", 2048);
        plugin.getConfig().addDefault("max-image-height", 2048);
        plugin.getConfig().addDefault("map-size", 128);
        plugin.getConfig().addDefault("async-processing", true);
        plugin.getConfig().addDefault("max-concurrent-tasks", 4);
        plugin.getConfig().addDefault("web-server.enabled", true);
        plugin.getConfig().addDefault("web-server.host", "0.0.0.0");
        plugin.getConfig().addDefault("web-server.port", 8080);
        plugin.getConfig().addDefault("web-server.public-url", "http://127.0.0.1:8080");
        
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

        this.webServerEnabled = plugin.getConfig().getBoolean("web-server.enabled", true);
        this.webServerHost = plugin.getConfig().getString("web-server.host", "0.0.0.0");
        this.webServerPort = plugin.getConfig().getInt("web-server.port", 8080);
        this.webPublicUrl = plugin.getConfig().getString("web-server.public-url", "https://map.yourserver.com");
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

    public File getPlayerImageDirectory(UUID playerUuid) {
        File dir = new File(imageDirectory, playerUuid.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public File getPlayerImageDirectory(Player player) {
        return getPlayerImageDirectory(player.getUniqueId());
    }

    public File getImageFile(String imageName) {
        File file = new File(imageDirectory, imageName);
        try {
            if (file.getCanonicalPath().startsWith(imageDirectory.getCanonicalPath())) {
                return file;
            }
        } catch (Exception ignored) {
        }
        return new File(imageDirectory, imageName);
    }

    public boolean isWebServerEnabled() {
        return webServerEnabled;
    }

    public String getWebServerHost() {
        return webServerHost;
    }

    public int getWebServerPort() {
        return webServerPort;
    }

    public String getWebPublicUrl() {
        return webPublicUrl;
    }
}

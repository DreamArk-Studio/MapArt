package com.mapart;

import com.mapart.command.MapArtCommand;
import com.mapart.config.PluginConfig;
import com.mapart.gui.MapArtGUI;
import com.mapart.listener.MapRenderListener;
import com.mapart.manager.MapArtManager;
import com.mapart.message.MessageManager;
import com.mapart.telemetry.TelemetryManager;
import com.mapart.upload.UploadTokenManager;
import com.mapart.upload.WebUploadServer;
import com.mapart.version.VersionChecker;
import org.bukkit.plugin.java.JavaPlugin;

public final class MapArtPlugin extends JavaPlugin {

    private PluginConfig config;
    private MapArtCommand mapArtCommand;
    private TelemetryManager telemetryManager;
    private MapArtManager manager;
    private MapArtGUI gui;
    private UploadTokenManager tokenManager;
    private WebUploadServer webUploadServer;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new PluginConfig(this);
        config.load();

        messageManager = new MessageManager(config.getLanguage());

        manager = new MapArtManager(this);
        gui = new MapArtGUI(this);
        tokenManager = new UploadTokenManager();
        webUploadServer = new WebUploadServer(this, tokenManager);

        mapArtCommand = new MapArtCommand(this);
        getCommand("mapart").setExecutor(mapArtCommand);
        getCommand("mapart").setTabCompleter(mapArtCommand);

        getServer().getPluginManager().registerEvents(new MapRenderListener(this), this);
        getServer().getPluginManager().registerEvents(gui, this);

        try {
            webUploadServer.start();
        } catch (Exception e) {
            getLogger().warning("Failed to start web upload server: " + e.getMessage());
        }

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                tokenManager.cleanup();
            } catch (Exception ignored) {
            }
        }, 20L * 30, 20L * 60);

        telemetryManager = new TelemetryManager(this);
        telemetryManager.init();

        VersionChecker versionChecker = new VersionChecker(this);
        getServer().getScheduler().runTaskLaterAsynchronously(this, versionChecker::check, 20L * 3);

        getLogger().info("MapArt has been enabled!");
    }

    @Override
    public void onDisable() {
        if (webUploadServer != null) {
            webUploadServer.stop();
        }
        if (mapArtCommand != null) {
            mapArtCommand.getManager().shutdown();
        }
        if (telemetryManager != null) {
            telemetryManager.shutdown();
        }
        getLogger().info("MapArt has been disabled!");
    }

    public PluginConfig getPluginConfig() {
        return config;
    }

    public MapArtManager getManager() {
        return manager;
    }

    public MapArtGUI getGui() {
        return gui;
    }

    public UploadTokenManager getTokenManager() {
        return tokenManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
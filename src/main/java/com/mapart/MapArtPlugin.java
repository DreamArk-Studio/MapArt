package com.mapart;

import com.mapart.command.MapArtCommand;
import com.mapart.config.PluginConfig;
import com.mapart.gui.MapArtGUI;
import com.mapart.listener.MapRenderListener;
import com.mapart.manager.MapArtManager;
import com.mapart.telemetry.TelemetryManager;
import com.mapart.upload.UploadTokenManager;
import com.mapart.upload.WebUploadServer;
import org.bukkit.plugin.java.JavaPlugin;

public final class MapArtPlugin extends JavaPlugin {

    private PluginConfig config;
    private MapArtCommand mapArtCommand;
    private TelemetryManager telemetryManager;
    private MapArtManager manager;
    private MapArtGUI gui;
    private UploadTokenManager tokenManager;
    private WebUploadServer webUploadServer;

    @Override
    public void onEnable() {
        getLogger().info("🏢筑梦方舟网络科技工作室版权所有 官网： https://www.dreamark.club/");
        getLogger().info("使用本插件默认您认可我们的隐私政策： https://www.dreamark.club/page.php?slug=privacy");
        getLogger().info("和用户协议： https://www.dreamark.club/page.php?slug=terms");

        saveDefaultConfig();
        config = new PluginConfig(this);
        config.load();

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

        telemetryManager = new TelemetryManager(this);
        telemetryManager.init();

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
}
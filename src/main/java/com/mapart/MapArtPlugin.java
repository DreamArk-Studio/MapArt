package com.mapart;

import com.mapart.command.MapArtCommand;
import com.mapart.config.PluginConfig;
import com.mapart.telemetry.TelemetryManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MapArtPlugin extends JavaPlugin {

    private PluginConfig config;
    private MapArtCommand mapArtCommand;
    private TelemetryManager telemetryManager;

    @Override
    public void onEnable() {
        getLogger().info("🏢筑梦方舟网络科技工作室版权所有 官网： https://www.dreamark.club/");
        getLogger().info("使用本插件默认您认可我们的隐私政策： https://www.dreamark.club/page.php?slug=privacy");
        getLogger().info("和用户协议： https://www.dreamark.club/page.php?slug=terms");

        saveDefaultConfig();
        config = new PluginConfig(this);
        config.load();

        mapArtCommand = new MapArtCommand(this);
        getCommand("mapart").setExecutor(mapArtCommand);
        getCommand("mapart").setTabCompleter(mapArtCommand);

        telemetryManager = new TelemetryManager(this);
        telemetryManager.init();

        getLogger().info("MapArt has been enabled!");
    }

    @Override
    public void onDisable() {
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
}

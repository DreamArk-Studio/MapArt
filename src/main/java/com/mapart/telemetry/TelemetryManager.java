package com.mapart.telemetry;

import com.mapart.MapArtPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 遥测管理器 - 负责 UUID 生成与心跳上报
 */
public class TelemetryManager {

    private static final String HEARTBEAT_URL = "https://tlm.dreamark.club/api/heartbeat.php";
    private static final String DREAMARK_DIR_NAME = "DreamArk";
    private static final String UUID_FILE_NAME = "server-uuid.txt";
    private static final long HEARTBEAT_INTERVAL_SECONDS = 60;

    private final MapArtPlugin plugin;
    private final ScheduledExecutorService scheduler;
    private String serverUuid;

    public TelemetryManager(MapArtPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MapArt-Telemetry");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 初始化遥测系统
     */
    public void init() {
        loadOrCreateUuid();
        startHeartbeat();
    }

    /**
     * 加载或生成 UUID
     */
    private void loadOrCreateUuid() {
        File serverRoot = plugin.getServer().getWorldContainer().getParentFile();
        File dreamArkDir = new File(serverRoot, DREAMARK_DIR_NAME);
        File uuidFile = new File(dreamArkDir, UUID_FILE_NAME);

        if (uuidFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(uuidFile))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    serverUuid = line.trim();
                    return;
                }
            } catch (IOException ignored) {
            }
        }

        // 生成新 UUID
        serverUuid = UUID.randomUUID().toString();

        // 持久化
        if (!dreamArkDir.exists()) {
            dreamArkDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(uuidFile))) {
            writer.write(serverUuid);
        } catch (IOException ignored) {
        }
    }

    /**
     * 启动心跳定时任务
     */
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(
            this::sendHeartbeat,
            0,
            HEARTBEAT_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    /**
     * 发送心跳
     */
    private void sendHeartbeat() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(HEARTBEAT_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            String jsonBody = "{\"uuid\":\"" + serverUuid + "\"}";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            connection.getResponseCode();
        } catch (IOException ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 关闭遥测系统
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    public String getServerUuid() {
        return serverUuid;
    }
}

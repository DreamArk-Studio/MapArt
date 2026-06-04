package com.mapart.version;

import com.mapart.MapArtPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class VersionChecker {

    private static final String API_URL = "https://dreamark.club/api/versions.php";
    private static final String DOWNLOAD_URL = "https://dreamark.club/work.php?id=16";
    private static final String PLUGIN_NAME = "MapArt";

    private final MapArtPlugin plugin;

    public VersionChecker(MapArtPlugin plugin) {
        this.plugin = plugin;
    }

    public void check() {
        CompletableFuture.runAsync(() -> {
            try {
                String remoteVersion = fetchRemoteVersion();
                if (remoteVersion == null) return;

                String localVersion = plugin.getDescription().getVersion();
                int result = compareVersions(localVersion, remoteVersion);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (result < 0) {
                        plugin.getLogger().warning("========== " + plugin.getMessageManager().get("version.update_header") + " ==========");
                        plugin.getLogger().warning(String.format(plugin.getMessageManager().get("version.update_current"), localVersion, remoteVersion));
                        plugin.getLogger().warning(String.format(plugin.getMessageManager().get("version.update_download"), DOWNLOAD_URL));
                        plugin.getLogger().warning("========================================");
                    } else if (result == 0) {
                        plugin.getLogger().info(String.format(plugin.getMessageManager().get("version.up_to_date"), localVersion));
                    } else {
                        plugin.getLogger().info(String.format(plugin.getMessageManager().get("version.snapshot"), localVersion, remoteVersion));
                    }
                });
            } catch (Exception e) {
                // 静默失败，不影响服务器启动
            }
        });
    }

    private String fetchRemoteVersion() throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            conn.disconnect();
        }

        return parseMapArtVersion(response.toString());
    }

    private String parseMapArtVersion(String json) {
        int mapArtIdx = json.indexOf("\"name\":\"" + PLUGIN_NAME + "\"");
        if (mapArtIdx < 0) {
            mapArtIdx = json.indexOf("\"name\": \"" + PLUGIN_NAME + "\"");
        }
        if (mapArtIdx < 0) return null;

        int versionIdx = json.indexOf("\"version\":", mapArtIdx);
        if (versionIdx < 0) {
            versionIdx = json.indexOf("\"version\" :", mapArtIdx);
        }
        if (versionIdx < 0) return null;

        int start = json.indexOf("\"", versionIdx + 11);
        if (start < 0) return null;
        int end = json.indexOf("\"", start + 1);
        if (end < 0) return null;

        return json.substring(start + 1, end);
    }

    private int compareVersions(String local, String remote) {
        String localClean = local.replace("v", "").trim();
        String remoteClean = remote.replace("v", "").trim();

        String[] localParts = localClean.split("\\.");
        String[] remoteParts = remoteClean.split("\\.");

        int maxLen = Math.max(localParts.length, remoteParts.length);
        for (int i = 0; i < maxLen; i++) {
            int localNum = i < localParts.length ? parseVersionPart(localParts[i]) : 0;
            int remoteNum = i < remoteParts.length ? parseVersionPart(remoteParts[i]) : 0;
            if (localNum < remoteNum) return -1;
            if (localNum > remoteNum) return 1;
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
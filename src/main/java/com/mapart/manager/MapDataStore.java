package com.mapart.manager;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 持久化存储地图像素数据到磁盘
 */
public class MapDataStore {

    private final File dataDirectory;
    private final Map<Integer, byte[]> cache = new HashMap<>();

    public MapDataStore(File dataDirectory) {
        this.dataDirectory = dataDirectory;
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
    }

    /**
     * 保存地图数据到磁盘
     */
    public void saveMapData(int mapId, byte[] data) {
        cache.put(mapId, data);
        File file = new File(dataDirectory, mapId + ".dat");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * 从磁盘加载地图数据
     */
    public byte[] loadMapData(int mapId) {
        if (cache.containsKey(mapId)) {
            return cache.get(mapId);
        }
        File file = new File(dataDirectory, mapId + ".dat");
        if (!file.exists()) {
            return null;
        }
        try {
            byte[] data = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(data);
            }
            cache.put(mapId, data);
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 移除地图数据
     */
    public void removeMapData(int mapId) {
        cache.remove(mapId);
        File file = new File(dataDirectory, mapId + ".dat");
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 加载所有已保存的地图数据到缓存
     */
    public void loadAll() {
        File[] files = dataDirectory.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return;
        for (File file : files) {
            try {
                String name = file.getName();
                int mapId = Integer.parseInt(name.substring(0, name.indexOf('.')));
                byte[] data = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(data);
                }
                cache.put(mapId, data);
            } catch (IOException | NumberFormatException ignored) {
            }
        }
    }

    public boolean hasData(int mapId) {
        return cache.containsKey(mapId) || new File(dataDirectory, mapId + ".dat").exists();
    }
}
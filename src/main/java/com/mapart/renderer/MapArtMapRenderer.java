package com.mapart.renderer;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * 自定义地图渲染器，将像素数据渲染到地图上
 */
public class MapArtMapRenderer extends MapRenderer {

    private final byte[] mapData;
    private final int mapSize;
    private boolean rendered;

    public MapArtMapRenderer(byte[] mapData, int mapSize) {
        super(false);
        this.mapData = mapData;
        this.mapSize = mapSize;
        this.rendered = false;
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if (rendered) return;
        rendered = true;
        for (int y = 0; y < mapSize; y++) {
            for (int x = 0; x < mapSize; x++) {
                mapCanvas.setPixel(x, y, mapData[y * mapSize + x]);
            }
        }
    }

    @Override
    public boolean isExplorerMap() {
        return false;
    }
}
package com.mapart.manager;

import com.mapart.MapArtPlugin;
import com.mapart.renderer.MapArtRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 管理地图画的创建和应用
 */
public class MapArtManager {

    private final MapArtPlugin plugin;
    private final MapArtRenderer renderer;
    private final ExecutorService executorService;
    
    // 存储已创建的地图画数据
    private final Map<Short, byte[]> mapArtData = new HashMap<>();

    public MapArtManager(MapArtPlugin plugin) {
        this.plugin = plugin;
        this.renderer = new MapArtRenderer();
        this.executorService = Executors.newFixedThreadPool(
            plugin.getPluginConfig().getMaxConcurrentTasks()
        );
    }

    /**
     * 从文件加载图片并创建地图画
     */
    public CompletableFuture<MapArtResult> createMapArt(Player player, String imageName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File imageFile = new File(plugin.getPluginConfig().getImageDirectory(), imageName);
                if (!imageFile.exists()) {
                    return new MapArtResult(false, "图片文件不存在: " + imageName);
                }

                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) {
                    return new MapArtResult(false, "无法读取图片文件，格式可能不支持");
                }

                // 检查图片尺寸
                if (image.getWidth() > plugin.getPluginConfig().getMaxImageWidth() ||
                    image.getHeight() > plugin.getPluginConfig().getMaxImageHeight()) {
                    return new MapArtResult(false, 
                        String.format("图片尺寸过大！最大允许: %dx%d", 
                            plugin.getPluginConfig().getMaxImageWidth(),
                            plugin.getPluginConfig().getMaxImageHeight()));
                }

                int mapSize = plugin.getPluginConfig().getMapSize();
                int mapCount = renderer.getRequiredMapCount(image, mapSize);
                
                // 渲染图片到地图
                byte[][] mapData = renderer.renderLargeImage(image, mapSize);
                
                // 创建地图物品
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (byte[] data : mapData) {
                        MapView mapView = Bukkit.createMap(player.getWorld());
                        mapView.setScale(MapView.Scale.CLOSEST);
                        mapView.setTrackingPosition(false);
                        mapView.setUnlimitedTracking(false);
                        
                        // 清除默认渲染器并添加自定义渲染器
                        mapView.removeRenderer(mapView.getRenderers().iterator().next());
                        mapView.addRenderer(new MapArtMapRenderer(data, mapSize));
                        
                        int id = mapView.getId();
                        mapArtData.put((short) id, data);
                        
                        // 给玩家地图物品
                        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                        MapMeta meta = (MapMeta) mapItem.getItemMeta();
                        meta.setMapView(mapView);
                        mapItem.setItemMeta(meta);
                        
                        player.getInventory().addItem(mapItem);
                    }
                });

                return new MapArtResult(true, 
                    String.format("成功创建地图画！共 %d 张地图", mapCount));
                    
            } catch (IOException e) {
                return new MapArtResult(false, "处理图片时出错: " + e.getMessage());
            }
        }, executorService);
    }

    /**
     * 清空玩家的地图画
     */
    public void clearMapArt(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.FILLED_MAP) {
                MapMeta meta = (MapMeta) item.getItemMeta();
                if (meta != null && meta.hasMapView()) {
                    int mapId = meta.getMapView().getId();
                    mapArtData.remove((short) mapId);
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    /**
     * 获取地图画信息
     */
    public String getMapArtInfo(Player player) {
        int mapCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.FILLED_MAP) {
                mapCount++;
            }
        }
        return String.format("你持有 %d 张地图", mapCount);
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    /**
     * 地图画渲染器
     */
    private static class MapArtMapRenderer extends MapRenderer {
        private final byte[] mapData;
        private final int mapSize;

        public MapArtMapRenderer(byte[] mapData, int mapSize) {
            super(false);
            this.mapData = mapData;
            this.mapSize = mapSize;
        }

        @Override
        public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
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

    /**
     * 地图画操作结果
     */
    public record MapArtResult(boolean success, String message) {}
}

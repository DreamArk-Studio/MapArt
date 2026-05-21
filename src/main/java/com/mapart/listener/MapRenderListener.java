package com.mapart.listener;

import com.mapart.MapArtPlugin;
import com.mapart.renderer.MapArtMapRenderer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

/**
 * 重启后重新将自定义渲染器附加到已保存的地图
 */
public class MapRenderListener implements Listener {

    private final MapArtPlugin plugin;

    public MapRenderListener(MapArtPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scanInventory(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            scanInventory(event.getInventory());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (isMapWithStoredData(current)) {
            attachRenderer(current);
        }
        ItemStack cursor = event.getCursor();
        if (isMapWithStoredData(cursor)) {
            attachRenderer(cursor);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        for (ItemStack item : event.getNewItems().values()) {
            if (isMapWithStoredData(item)) {
                attachRenderer(item);
            }
        }
    }

    private void scanInventory(Player player) {
        scanInventory(player.getInventory());
    }

    private void scanInventory(org.bukkit.inventory.Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (isMapWithStoredData(item)) {
                attachRenderer(item);
            }
        }
    }

    private boolean isMapWithStoredData(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP) return false;
        MapMeta meta = (MapMeta) item.getItemMeta();
        if (meta == null || !meta.hasMapView()) return false;
        MapView mapView = meta.getMapView();
        if (mapView == null) return false;

        int mapId = mapView.getId();
        return plugin.getManager().getDataStore().hasData(mapId);
    }

    private void attachRenderer(ItemStack item) {
        MapMeta meta = (MapMeta) item.getItemMeta();
        if (meta == null || !meta.hasMapView()) return;
        MapView mapView = meta.getMapView();
        if (mapView == null) return;

        int mapSize = plugin.getPluginConfig().getMapSize();
        int mapId = mapView.getId();
        byte[] data = plugin.getManager().getDataStore().loadMapData(mapId);
        if (data == null) return;

        boolean hasCustomRenderer = mapView.getRenderers().stream()
                .anyMatch(r -> r instanceof MapArtMapRenderer);
        if (hasCustomRenderer) return;

        mapView.setScale(MapView.Scale.CLOSEST);
        mapView.setTrackingPosition(false);
        mapView.setUnlimitedTracking(false);

        mapView.getRenderers().forEach(mapView::removeRenderer);
        mapView.addRenderer(new MapArtMapRenderer(data, mapSize));
    }
}
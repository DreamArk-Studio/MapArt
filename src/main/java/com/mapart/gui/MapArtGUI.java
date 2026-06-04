package com.mapart.gui;

import com.mapart.MapArtPlugin;
import com.mapart.message.MessageManager;
import com.mapart.renderer.MapArtRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class MapArtGUI implements Listener {

    private static final int GUI_SIZE = 54;

    private final MapArtPlugin plugin;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, Map<Integer, String>> playerSlotPaths = new HashMap<>();

    public MapArtGUI(MapArtPlugin plugin) {
        this.plugin = plugin;
    }

    private MessageManager msg() {
        return plugin.getMessageManager();
    }

    private String getGuiTitle() {
        return msg().get("gui.title");
    }

    public void open(Player player) {
        playerPages.put(player.getUniqueId(), 0);
        openPage(player, 0);
    }

    private void openPage(Player player, int page) {
        File imagesRoot = plugin.getPluginConfig().getImageDirectory();
        File playerDir = plugin.getPluginConfig().getPlayerImageDirectory(player.getUniqueId());
        List<ImageEntry> entries = new ArrayList<>();
        collectImageFiles(imagesRoot, playerDir, entries);
        entries.sort(Comparator.comparingLong(e -> e.file.lastModified()));

        int itemsPerPage = 28;
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / itemsPerPage));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), safePage);

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, getGuiTitle() + " §7- " + (safePage + 1) + "/" + totalPages);

        int startIndex = safePage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, entries.size());

        Map<Integer, String> slotMap = new HashMap<>();

        for (int i = startIndex, slot = 0; i < endIndex; i++, slot++) {
            ImageEntry entry = entries.get(i);
            ItemStack item = new ItemStack(Material.ITEM_FRAME);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + entry.file.getName());
            List<String> lore = new ArrayList<>();
            lore.add(msg().get("gui.click_to_use"));
            lore.add(msg().get("gui.size") + formatFileSize(entry.file.length()));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slotMap.put(slot, entry.relativePath);
        }

        playerSlotPaths.put(player.getUniqueId(), slotMap);

        if (safePage > 0) {
            inv.setItem(45, buildItem(Material.BOOKSHELF, msg().get("gui.first_page")));
            inv.setItem(48, buildItem(Material.ARROW, msg().get("gui.prev_page")));
        }
        if (safePage < totalPages - 1) {
            inv.setItem(50, buildItem(Material.ARROW, msg().get("gui.next_page")));
            inv.setItem(53, buildItem(Material.BOOK, msg().get("gui.last_page")));
        }

        inv.setItem(49, buildItem(Material.LIME_WOOL, msg().get("gui.upload_btn"), msg().get("gui.upload_lore1"), msg().get("gui.upload_lore2")));
        inv.setItem(52, buildItem(Material.PAPER, msg().get("gui.page_info") + (safePage + 1) + "/" + totalPages,
                msg().get("gui.total_images") + entries.size() + msg().get("gui.total_images_suffix"),
                msg().get("gui.per_page") + itemsPerPage + msg().get("gui.per_page_suffix")));

        player.openInventory(inv);
    }

    private void collectImageFiles(File root, File dir, List<ImageEntry> out) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                collectImageFiles(root, file, out);
                continue;
            }
            String lower = file.getName().toLowerCase();
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp")) {
                String relative = root.toPath().relativize(file.toPath()).toString().replace("\\", "/");
                out.add(new ImageEntry(file, relative));
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(getGuiTitle())) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        int slot = event.getRawSlot();
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == 45 && clicked.getType() == Material.BOOKSHELF) {
            openPage(player, 0);
            return;
        }
        if (slot == 48 && clicked.getType() == Material.ARROW) {
            openPage(player, page - 1);
            return;
        }
        if (slot == 50 && clicked.getType() == Material.ARROW) {
            openPage(player, page + 1);
            return;
        }
        if (slot == 53 && clicked.getType() == Material.BOOK) {
            openPage(player, Integer.MAX_VALUE);
            return;
        }
        if (slot == 49 && clicked.getType() == Material.LIME_WOOL) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("mapart upload"));
            return;
        }

        if (slot >= 0 && slot < 45 && meta.getDisplayName().startsWith("§e")) {
            Map<Integer, String> slotMap = playerSlotPaths.get(player.getUniqueId());
            String relativePath = slotMap != null ? slotMap.get(slot) : null;
            if (relativePath == null) {
                player.sendMessage(msg().get("gui.path_error"));
                return;
            }

            player.closeInventory();
            String finalPath = relativePath;
            player.sendMessage(msg().get("gui.creating") + finalPath);

            plugin.getManager().createMapArt(player, finalPath, MapArtRenderer.Mode.SCALE).thenAccept(result -> {
                if (result.success()) {
                    player.sendMessage("§a" + result.message());
                } else {
                    player.sendMessage("§c" + result.message());
                }
            });
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (event.getView().getTitle().startsWith(getGuiTitle())) {
            playerPages.remove(uuid);
            playerSlotPaths.remove(uuid);
        }
    }

    private ItemStack buildItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) {
            meta.setLore(Arrays.asList(loreLines));
        }
        item.setItemMeta(meta);
        return item;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private static class ImageEntry {
        final File file;
        final String relativePath;

        ImageEntry(File file, String relativePath) {
            this.file = file;
            this.relativePath = relativePath;
        }
    }
}
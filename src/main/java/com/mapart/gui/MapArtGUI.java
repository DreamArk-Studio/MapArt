package com.mapart.gui;

import com.mapart.MapArtPlugin;
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

    private static final String GUI_TITLE = "§6§lMapArt 地图画";
    private static final int GUI_SIZE = 54;
    private static final String RELATIVE_PREFIX = "§8";

    private final MapArtPlugin plugin;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public MapArtGUI(MapArtPlugin plugin) {
        this.plugin = plugin;
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

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE + " §7- " + (safePage + 1) + "/" + totalPages);

        int startIndex = safePage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, entries.size());

        for (int i = startIndex, slot = 0; i < endIndex; i++, slot++) {
            ImageEntry entry = entries.get(i);
            ItemStack item = new ItemStack(Material.ITEM_FRAME);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + entry.file.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7点击使用此图片创建地图画");
            lore.add("§7大小: " + formatFileSize(entry.file.length()));
            lore.add(RELATIVE_PREFIX + entry.relativePath);
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }

        if (safePage > 0) {
            inv.setItem(45, buildItem(Material.BOOKSHELF, "§a⏮ 首页"));
            inv.setItem(48, buildItem(Material.ARROW, "§a◀ 上一页"));
        }
        if (safePage < totalPages - 1) {
            inv.setItem(50, buildItem(Material.ARROW, "§a下一页 ▶"));
            inv.setItem(53, buildItem(Material.BOOK, "§a末页 ⏭"));
        }

        inv.setItem(49, buildItem(Material.LIME_WOOL, "§a§l📤 上传图片", "§7点击获取上传链接", "§7通过网页上传新图片"));
        inv.setItem(52, buildItem(Material.PAPER, "§e📄 " + (safePage + 1) + "/" + totalPages,
                "§7共 " + entries.size() + " 张图片", "§7每页 " + itemsPerPage + " 张"));

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
        if (!event.getView().getTitle().startsWith(GUI_TITLE)) return;
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
            String relativePath = null;
            if (meta.hasLore()) {
                for (String line : meta.getLore()) {
                    if (line.startsWith(RELATIVE_PREFIX)) {
                        relativePath = line.substring(RELATIVE_PREFIX.length());
                        break;
                    }
                }
            }
            if (relativePath == null) {
                relativePath = player.getUniqueId() + "/" + meta.getDisplayName().substring(2);
            }

            player.closeInventory();
            String finalPath = relativePath;
            player.sendMessage("§a正在创建地图画: " + finalPath);

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
        if (event.getView().getTitle().startsWith(GUI_TITLE)) {
            playerPages.remove(event.getPlayer().getUniqueId());
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

    private record ImageEntry(File file, String relativePath) {}
}
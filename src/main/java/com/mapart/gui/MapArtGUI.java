package com.mapart.gui;

import com.mapart.MapArtPlugin;
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
        List<File> imageList = listPlayerImages(player);
        int itemsPerPage = 28;
        int totalPages = Math.max(1, (int) Math.ceil((double) imageList.size() / itemsPerPage));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), safePage);

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE + " §7- 第" + (safePage + 1) + "/" + totalPages + "页");

        int startIndex = safePage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, imageList.size());
        int slot = 0;

        for (int i = startIndex; i < endIndex; i++) {
            File img = imageList.get(i);
            ItemStack item = new ItemStack(Material.ITEM_FRAME);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + img.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7点击使用此图片创建地图画");
            lore.add("§7大小: " + formatFileSize(img.length()));
            lore.add("§8" + img.getPath());
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        if (safePage > 0) {
            inv.setItem(45, buildSimpleItem(Material.BOOKSHELF, "§a首页"));
            inv.setItem(48, buildSimpleItem(Material.ARROW, "§a上一页"));
        }

        if (safePage < totalPages - 1) {
            inv.setItem(50, buildSimpleItem(Material.ARROW, "§a下一页"));
            inv.setItem(53, buildSimpleItem(Material.BOOK, "§a末页"));
        }

        ItemStack uploadBtn = new ItemStack(Material.LIME_WOOL);
        ItemMeta uploadMeta = uploadBtn.getItemMeta();
        uploadMeta.setDisplayName("§a§l📤 上传图片");
        List<String> uploadLore = new ArrayList<>();
        uploadLore.add("§7点击获取上传链接");
        uploadLore.add("§7通过网页上传新图片");
        uploadMeta.setLore(uploadLore);
        uploadBtn.setItemMeta(uploadMeta);
        inv.setItem(49, uploadBtn);

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageInfoMeta = pageInfo.getItemMeta();
        pageInfoMeta.setDisplayName("§e📄 当前页 " + (safePage + 1) + "/" + totalPages);
        List<String> pageInfoLore = new ArrayList<>();
        pageInfoLore.add("§7共 " + imageList.size() + " 张图片");
        pageInfoLore.add("§7每页显示 " + itemsPerPage + " 张");
        pageInfoMeta.setLore(pageInfoLore);
        pageInfo.setItemMeta(pageInfoMeta);
        inv.setItem(52, pageInfo);

        player.openInventory(inv);
    }

    private List<File> listPlayerImages(Player player) {
        File imageDir = plugin.getPluginConfig().getPlayerImageDirectory(player.getUniqueId());
        List<File> result = new ArrayList<>();
        collectImageFiles(imageDir, result);
        result.sort(Comparator.comparingLong(File::lastModified).reversed());
        return result;
    }

    private void collectImageFiles(File dir, List<File> out) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                collectImageFiles(file, out);
                continue;
            }
            String lower = file.getName().toLowerCase();
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp")) {
                out.add(file);
            }
        }
    }

    private ItemStack buildSimpleItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(GUI_TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getRawSlot();
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == 48 && clicked.getType() == Material.ARROW) {
            playerPages.put(player.getUniqueId(), page - 1);
            openPage(player, page - 1);
            return;
        }

        if (slot == 50 && clicked.getType() == Material.ARROW) {
            playerPages.put(player.getUniqueId(), page + 1);
            openPage(player, page + 1);
            return;
        }

        if (slot == 45 && clicked.getType() == Material.BOOKSHELF) {
            playerPages.put(player.getUniqueId(), 0);
            openPage(player, 0);
            return;
        }

        if (slot == 53 && clicked.getType() == Material.BOOK) {
            playerPages.put(player.getUniqueId(), Integer.MAX_VALUE);
            openPage(player, Integer.MAX_VALUE);
            return;
        }

        if (slot == 49 && clicked.getType() == Material.LIME_WOOL) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.performCommand("mapart upload"));
            return;
        }

        if (slot >= 0 && slot < 45) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;
            String name = meta.getDisplayName();
            if (name.startsWith("§e")) {
                String fileName = name.substring(2);
                if (meta.hasLore()) {
                    for (String line : meta.getLore()) {
                        if (line.startsWith("§8")) {
                            File file = new File(line.substring(2));
                            try {
                                File root = plugin.getPluginConfig().getImageDirectory();
                                fileName = root.toPath().relativize(file.toPath()).toString().replace("\\", "/");
                            } catch (Exception ignored) {
                            }
                            break;
                        }
                    }
                }
                player.closeInventory();
                String finalFileName = fileName;
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.performCommand("mapart apply " + finalFileName));
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith(GUI_TITLE)) {
            playerPages.remove(event.getPlayer().getUniqueId());
        }
    }

    private String formatFileSize(long bytes) {
        String[] units = {"B", "KB", "MB"};
        int i = 0;
        double size = bytes;
        while (size >= 1024 && i < 2) {
            size /= 1024;
            i++;
        }
        return String.format("%.1f %s", size, units[i]);
    }
}
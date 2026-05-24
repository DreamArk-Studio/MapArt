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
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE + " §7- 第" + (page + 1) + "页");

        File[] images = plugin.getPluginConfig().getImageDirectory().listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp");
        });

        List<File> imageList = images != null ? Arrays.asList(images) : Collections.emptyList();
        imageList.sort(Comparator.comparingLong(File::lastModified).reversed());

        int itemsPerPage = 28;
        int startIndex = page * itemsPerPage;
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
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("§a上一页");
            prev.setItemMeta(prevMeta);
            inv.setItem(48, prev);
        }

        if (endIndex < imageList.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§a下一页");
            next.setItemMeta(nextMeta);
            inv.setItem(50, next);
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

        ItemStack infoBtn = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoBtn.getItemMeta();
        infoMeta.setDisplayName("§e📋 使用帮助");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7选择图片后自动创建地图画");
        infoLore.add("§7地图会直接放入你的背包");
        infoMeta.setLore(infoLore);
        infoBtn.setItemMeta(infoMeta);
        inv.setItem(53, infoBtn);

        player.openInventory(inv);
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
package com.mapart.command;

import com.mapart.MapArtPlugin;
import com.mapart.manager.MapArtManager;
import com.mapart.renderer.MapArtRenderer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapArtCommand implements CommandExecutor, TabCompleter {

    private final MapArtPlugin plugin;
    private final MapArtManager manager;

    public MapArtCommand(MapArtPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行！");
            return true;
        }

        if (args.length == 0) {
            player.performCommand("mapart gui");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> handleGui(player);
            case "upload" -> handleUpload(player);
            case "apply" -> handleApply(player, args);
            case "clear" -> handleClear(player);
            case "info" -> handleInfo(player);
            case "list" -> handleList(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleGui(Player player) {
        plugin.getGui().open(player);
    }

    private void handleUpload(Player player) {
        String token = plugin.getTokenManager().createToken(player.getUniqueId(), player.getName());
        String uploadUrl = plugin.getPluginConfig().getWebPublicUrl() + "/upload?token=" + token;
        player.sendMessage("§a点击链接上传图片（有效期为5分钟）：");
        player.sendMessage("§6§n" + uploadUrl);
        player.sendMessage("§7提示：上传成功后使用 §e/mapart gui §7查看并使用图片");
    }

    private void handleApply(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /mapart apply <图片文件名> [scale|tile]");
            player.sendMessage("§c  scale  - 缩放到单张地图（默认）");
            player.sendMessage("§c  tile    - 切分为多张地图");
            player.sendMessage("§c示例: /mapart apply myimage.png");
            player.sendMessage("§c示例: /mapart apply myimage.png tile");
            return;
        }

        MapArtRenderer.Mode mode = MapArtRenderer.Mode.SCALE;
        if (args.length >= 3) {
            String modeArg = args[2].toLowerCase();
            if (modeArg.equals("tile")) {
                mode = MapArtRenderer.Mode.TILE;
            } else if (!modeArg.equals("scale")) {
                player.sendMessage("§c无效模式，可用: scale, tile");
                return;
            }
        }

        String imageName = args[1];
        player.sendMessage("§a正在处理图片: " + imageName);

        manager.createMapArt(player, imageName, mode).thenAccept(result -> {
            if (result.success()) {
                player.sendMessage("§a" + result.message());
            } else {
                player.sendMessage("§c" + result.message());
            }
        });
    }

    private void handleClear(Player player) {
        manager.clearMapArt(player);
        player.sendMessage("§a已清除所有地图画！");
    }

    private void handleInfo(Player player) {
        player.sendMessage("§a" + manager.getMapArtInfo(player));
    }

    private void handleList(Player player) {
        File imageDir = plugin.getPluginConfig().getImageDirectory();
        if (!imageDir.exists() || imageDir.listFiles() == null) {
            player.sendMessage("§c图片目录为空！");
            return;
        }

        File[] images = imageDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || 
                   lower.endsWith(".jpeg") || lower.endsWith(".gif") || 
                   lower.endsWith(".bmp") || lower.endsWith(".webp");
        });

        if (images == null || images.length == 0) {
            player.sendMessage("§c没有找到可用的图片！");
            return;
        }

        player.sendMessage("§a可用的图片文件:");
        for (File image : images) {
            player.sendMessage("§7- " + image.getName());
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== MapArt 帮助 ===");
        player.sendMessage("§e/mapart §7- 打开图形界面");
        player.sendMessage("§e/mapart gui §7- 打开图形界面");
        player.sendMessage("§e/mapart upload §7- 获取网页上传链接");
        player.sendMessage("§e/mapart apply <图片> [scale|tile] §7- 将图片转换为地图画");
        player.sendMessage("§e/mapart clear §7- 清除所有地图画");
        player.sendMessage("§e/mapart info §7- 查看地图画信息");
        player.sendMessage("§e/mapart list §7- 列出可用的图片");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("gui", "upload", "apply", "clear", "info", "list");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("apply")) {
            File imageDir = plugin.getPluginConfig().getImageDirectory();
            if (imageDir.exists() && imageDir.listFiles() != null) {
                return Arrays.stream(imageDir.listFiles())
                        .filter(File::isFile)
                        .map(File::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("apply")) {
            return Arrays.asList("scale", "tile").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    public MapArtManager getManager() {
        return manager;
    }
}
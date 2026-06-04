package com.mapart.command;

import com.mapart.MapArtPlugin;
import com.mapart.manager.MapArtManager;
import com.mapart.message.MessageManager;
import com.mapart.renderer.MapArtRenderer;
import org.bukkit.Bukkit;
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

    private MessageManager msg() {
        return plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg().get("cmd.player_only"));
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
            case "list" -> handleList(player, args.length > 1 ? args[1] : "mine");
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(uploadUrl).openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                conn.disconnect();
            } catch (Exception e) {
                player.sendMessage(msg().get("cmd.upload_connect_fail"));
            }
        });
        player.sendMessage(msg().get("cmd.upload_click_link"));
        var clickMsg = new net.md_5.bungee.api.chat.TextComponent(msg().get("cmd.upload_click_url"));
        clickMsg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, uploadUrl));
        clickMsg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.hover.content.Text(msg().get("cmd.upload_hover") + uploadUrl)
        ));
        player.spigot().sendMessage(clickMsg);
        player.sendMessage(msg().get("cmd.upload_fallback"));
        player.sendMessage("§f" + uploadUrl);
        player.sendMessage(msg().get("cmd.upload_hint"));
    }

    private void handleApply(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(msg().get("cmd.apply_usage_1"));
            player.sendMessage(msg().get("cmd.apply_usage_2"));
            player.sendMessage(msg().get("cmd.apply_usage_3"));
            player.sendMessage(msg().get("cmd.apply_usage_4"));
            player.sendMessage(msg().get("cmd.apply_usage_5"));
            return;
        }

        MapArtRenderer.Mode mode = MapArtRenderer.Mode.SCALE;
        if (args.length >= 3) {
            String modeArg = args[2].toLowerCase();
            if (modeArg.equals("tile")) {
                mode = MapArtRenderer.Mode.TILE;
            } else if (!modeArg.equals("scale")) {
                player.sendMessage(msg().get("cmd.apply_invalid_mode"));
                return;
            }
        }

        String input = args[1];
        String resolved = resolveImageName(player, input);
        if (resolved == null) {
            player.sendMessage(msg().get("cmd.apply_not_found") + input);
            player.sendMessage(msg().get("cmd.apply_upload_hint"));
            return;
        }

        player.sendMessage(msg().get("cmd.apply_processing") + resolved);

        manager.createMapArt(player, resolved, mode).thenAccept(result -> {
            if (result.success()) {
                player.sendMessage("§a" + result.message());
            } else {
                player.sendMessage("§c" + result.message());
            }
        });
    }

    private void handleClear(Player player) {
        manager.clearMapArt(player);
        player.sendMessage(msg().get("cmd.clear_done"));
    }

    private void handleInfo(Player player) {
        player.sendMessage("§a" + manager.getMapArtInfo(player));
    }

    private void handleList(Player player) {
        handleList(player, "mine");
    }

    private void handleList(Player player, String scope) {
        File playerDir = plugin.getPluginConfig().getPlayerImageDirectory(player);
        File globalDir = plugin.getPluginConfig().getImageDirectory();
        boolean showMine = scope.equalsIgnoreCase("mine") || scope.equalsIgnoreCase("all");
        boolean showGlobal = scope.equalsIgnoreCase("global") || scope.equalsIgnoreCase("all");

        List<String> names = new ArrayList<>();
        if (showMine) {
            addImageFiles(playerDir, playerDir, names);
        }
        if (showGlobal) {
            addImageFiles(globalDir, globalDir, names);
        }

        if (names.isEmpty()) {
            player.sendMessage(msg().get("cmd.list_not_found"));
            player.sendMessage(msg().get("cmd.list_upload_hint"));
            return;
        }

        player.sendMessage(msg().get("cmd.list_available"));
        for (String name : names) {
            player.sendMessage("§7- " + name);
        }
    }

    private void addImageFiles(File root, File dir, List<String> out) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                addImageFiles(root, file, out);
                continue;
            }
            String lower = file.getName().toLowerCase();
            if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                  lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp"))) {
                continue;
            }
            try {
                String relative = root.toPath().relativize(file.toPath()).toString();
                out.add(relative.replace("\\", "/"));
            } catch (Exception ignored) {
                out.add(file.getName());
            }
        }
    }

    private String resolveImageName(Player player, String input) {
        if (input == null || input.isBlank()) return null;
        String normalized = input.replace("\\", "/");

        if (normalized.contains("/")) {
            File direct = plugin.getPluginConfig().getImageFile(normalized);
            if (isFileAccessible(direct)) {
                return normalized;
            }
            return null;
        }

        File playerFile = new File(plugin.getPluginConfig().getPlayerImageDirectory(player), normalized);
        if (isFileAccessible(playerFile)) {
            return player.getUniqueId() + "/" + normalized;
        }

        File imagesRoot = plugin.getPluginConfig().getImageDirectory();
        File[] dirs = imagesRoot.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                File candidate = new File(dir, normalized);
                if (isFileAccessible(candidate)) {
                    return dir.getName() + "/" + normalized;
                }
            }
        }

        File globalFile = new File(imagesRoot, normalized);
        if (isFileAccessible(globalFile)) {
            return normalized;
        }

        return null;
    }

    private boolean isFileAccessible(File file) {
        if (file.exists()) return true;
        try {
            return java.nio.file.Files.isReadable(file.toPath());
        } catch (Exception e) {
            return false;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(msg().get("cmd.help_title"));
        player.sendMessage(msg().get("cmd.help_gui"));
        player.sendMessage(msg().get("cmd.help_gui2"));
        player.sendMessage(msg().get("cmd.help_upload"));
        player.sendMessage(msg().get("cmd.help_apply"));
        player.sendMessage(msg().get("cmd.help_clear"));
        player.sendMessage(msg().get("cmd.help_info"));
        player.sendMessage(msg().get("cmd.help_list"));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return null;
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("gui", "upload", "apply", "clear", "info", "list");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return Arrays.asList("mine", "all", "global").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("apply")) {
            File imageDir = plugin.getPluginConfig().getPlayerImageDirectory(player.getUniqueId());
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
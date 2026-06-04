package com.mapart.message;

import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final Map<String, String> messages = new HashMap<>();
    private final String language;

    public MessageManager(String language) {
        this.language = (language == null ? "zh_cn" : language).toLowerCase();
        loadMessages();
    }

    private void loadMessages() {
        if ("en_us".equals(language)) {
            loadEnglish();
        } else {
            loadChinese();
        }
    }

    public String get(String key) {
        return messages.getOrDefault(key, key);
    }

    public String get(String key, Object... args) {
        return String.format(get(key), args);
    }

    public String getLanguage() {
        return language;
    }

    // ========== Command Messages ==========
    private void loadEnglish() {
        messages.put("cmd.player_only", "§cThis command can only be used by players!");
        messages.put("cmd.upload_connect_fail", "§c⚠ Cannot connect to upload server, please check port and firewall");
        messages.put("cmd.upload_click_link", "§aClick the link to upload images (valid for 5 minutes):");
        messages.put("cmd.upload_click_url", "§6§nClick to open upload page");
        messages.put("cmd.upload_hover", "Click to open upload link\n");
        messages.put("cmd.upload_fallback", "§7If link is not clickable, copy the URL below to browser:");
        messages.put("cmd.upload_hint", "§7Tip: Use §e/mapart gui §7after upload to view and use images");
        messages.put("cmd.apply_usage_1", "§cUsage: /mapart apply <filename> [scale|tile]");
        messages.put("cmd.apply_usage_2", "§c  scale  - Scale to single map (default)");
        messages.put("cmd.apply_usage_3", "§c  tile    - Split into multiple maps");
        messages.put("cmd.apply_usage_4", "§cExample: /mapart apply myimage.png");
        messages.put("cmd.apply_usage_5", "§cExample: /mapart apply myimage.png tile");
        messages.put("cmd.apply_invalid_mode", "§cInvalid mode, available: scale, tile");
        messages.put("cmd.apply_not_found", "§cImage file not found: ");
        messages.put("cmd.apply_upload_hint", "§7Please upload first via §e/mapart upload");
        messages.put("cmd.apply_processing", "§aProcessing image: ");
        messages.put("cmd.clear_done", "§aAll map art cleared!");
        messages.put("cmd.list_not_found", "§cNo images found!");
        messages.put("cmd.list_upload_hint", "§7Please upload first via §e/mapart upload");
        messages.put("cmd.list_available", "§aAvailable image files:");
        messages.put("cmd.help_title", "§6§l=== MapArt Help ===");
        messages.put("cmd.help_gui", "§e/mapart §7- Open GUI");
        messages.put("cmd.help_gui2", "§e/mapart gui §7- Open GUI");
        messages.put("cmd.help_upload", "§e/mapart upload §7- Get web upload link");
        messages.put("cmd.help_apply", "§e/mapart apply <image> [scale|tile] §7- Convert image to map art");
        messages.put("cmd.help_clear", "§e/mapart clear §7- Clear all map art");
        messages.put("cmd.help_info", "§e/mapart info §7- View map art info");
        messages.put("cmd.help_list", "§e/mapart list §7- List available images");

        // GUI Messages
        messages.put("gui.title", "§6§lMapArt Maps");
        messages.put("gui.click_to_use", "§7Click to use this image");
        messages.put("gui.size", "§7Size: ");
        messages.put("gui.first_page", "§a⏮ First");
        messages.put("gui.prev_page", "§a◀ Prev");
        messages.put("gui.next_page", "§aNext ▶");
        messages.put("gui.last_page", "§aLast ⏭");
        messages.put("gui.upload_btn", "§a§l📤 Upload Image");
        messages.put("gui.upload_lore1", "§7Click to get upload link");
        messages.put("gui.upload_lore2", "§7Upload via web page");
        messages.put("gui.page_info", "§e📄 ");
        messages.put("gui.total_images", "§7Total ");
        images_suffix: messages.put("gui.total_images_suffix", " images");
        messages.put("gui.per_page", "§7Per page ");
        messages.put("gui.per_page_suffix", " images");
        messages.put("gui.path_error", "§cCannot get image path, please try again");
        messages.put("gui.creating", "§aCreating map art: ");

        // Manager Messages
        messages.put("mgr.read_error", "Cannot read image file: ");
        messages.put("mgr.read_unsupported", "Cannot read image, format may be unsupported: ");
        messages.put("mgr.size_too_large", "Image too large! Max allowed: %dx%d");
        messages.put("mgr.mode_scale", "scale");
        messages.put("mgr.mode_tile", "tile");
        messages.put("mgr.success", "Map art created (%s mode)! Total %d maps");
        messages.put("mgr.error", "Error processing image: ");
        messages.put("mgr.info", "You hold %d maps");

        // Version Checker Messages
        messages.put("version.update_header", "========== MapArt Update Available ==========");
        messages.put("version.update_current", "Current version: v%s | Latest: %s");
        messages.put("version.update_download", "Please download latest version: %s");
        messages.put("version.up_to_date", "[MapArt] Your plugin is up to date (%s)");
        messages.put("version.snapshot", "[MapArt] Your plugin is a snapshot version (current: v%s, official: %s)");

        // Web Upload Page Messages
        messages.put("web.title", "MapArt Image Upload");
        messages.put("web.subtitle", "Upload images to server for map art");
        messages.put("web.drop_text", "Click to select or drag image here");
        messages.put("web.drop_hint", "Supports PNG / JPG / GIF / BMP / WEBP");
        messages.put("web.upload_btn", "Upload Image");
        messages.put("web.uploading", "Uploading...");
        messages.put("web.footer", "MapArt Plugin &copy; DreamArk Studio");
        messages.put("web.success", "Upload successful! Image saved. Return to game and use /mapart gui");
        messages.put("web.invalid_token", "Invalid token, please execute /mapart upload in game for a new link");
        messages.put("web.invalid_type", "Unsupported file type, please upload PNG/JPG/GIF/BMP/WEBP image");
        messages.put("web.size_exceeded", "Image size exceeded (max 2048x2048)");
        messages.put("web.virus_detected", "File verification failed, please ensure you uploaded a valid image");
        messages.put("web.alert_invalid_type", "Unsupported file type");
        messages.put("web.alert_file_too_large", "File too large, max 10MB");
    }

    private void loadChinese() {
        messages.put("cmd.player_only", "§c该命令只能由玩家执行！");
        messages.put("cmd.upload_connect_fail", "§c⚠ 无法连接到上传服务器，请检查端口和防火墙是否放行 8080");
        messages.put("cmd.upload_click_link", "§a点击链接上传图片（有效期5分钟）：");
        messages.put("cmd.upload_click_url", "§6§n点击打开上传页面");
        messages.put("cmd.upload_hover", "点击打开上传链接\n");
        messages.put("cmd.upload_fallback", "§7如果链接不可点击，请复制下方网址到浏览器打开：");
        messages.put("cmd.upload_hint", "§7提示：上传成功后使用 §e/mapart gui §7查看并使用图片");
        messages.put("cmd.apply_usage_1", "§c用法: /mapart apply <图片文件名> [scale|tile]");
        messages.put("cmd.apply_usage_2", "§c  scale  - 缩放到单张地图（默认）");
        messages.put("cmd.apply_usage_3", "§c  tile    - 切分为多张地图");
        messages.put("cmd.apply_usage_4", "§c示例: /mapart apply myimage.png");
        messages.put("cmd.apply_usage_5", "§c示例: /mapart apply myimage.png tile");
        messages.put("cmd.apply_invalid_mode", "§c无效模式，可用: scale, tile");
        messages.put("cmd.apply_not_found", "§c图片文件不存在: ");
        messages.put("cmd.apply_upload_hint", "§7请先通过 §e/mapart upload §7上传图片");
        messages.put("cmd.apply_processing", "§a正在处理图片: ");
        messages.put("cmd.clear_done", "§a已清除所有地图画！");
        messages.put("cmd.list_not_found", "§c没有找到可用的图片！");
        messages.put("cmd.list_upload_hint", "§7请先通过 §e/mapart upload §7上传图片");
        messages.put("cmd.list_available", "§a可用的图片文件:");
        messages.put("cmd.help_title", "§6§l=== MapArt 帮助 ===");
        messages.put("cmd.help_gui", "§e/mapart §7- 打开图形界面");
        messages.put("cmd.help_gui2", "§e/mapart gui §7- 打开图形界面");
        messages.put("cmd.help_upload", "§e/mapart upload §7- 获取网页上传链接");
        messages.put("cmd.help_apply", "§e/mapart apply <图片> [scale|tile] §7- 将图片转换为地图画");
        messages.put("cmd.help_clear", "§e/mapart clear §7- 清除所有地图画");
        messages.put("cmd.help_info", "§e/mapart info §7- 查看地图画信息");
        messages.put("cmd.help_list", "§e/mapart list §7- 列出可用的图片");

        // GUI Messages
        messages.put("gui.title", "§6§lMapArt 地图画");
        messages.put("gui.click_to_use", "§7点击使用此图片创建地图画");
        messages.put("gui.size", "§7大小: ");
        messages.put("gui.first_page", "§a⏮ 首页");
        messages.put("gui.prev_page", "§a◀ 上一页");
        messages.put("gui.next_page", "§a下一页 ▶");
        messages.put("gui.last_page", "§a末页 ⏭");
        messages.put("gui.upload_btn", "§a§l📤 上传图片");
        messages.put("gui.upload_lore1", "§7点击获取上传链接");
        messages.put("gui.upload_lore2", "§7通过网页上传新图片");
        messages.put("gui.page_info", "§e📄 ");
        messages.put("gui.total_images", "§7共 ");
        messages.put("gui.total_images_suffix", " 张图片");
        messages.put("gui.per_page", "§7每页 ");
        messages.put("gui.per_page_suffix", " 张");
        messages.put("gui.path_error", "§c无法获取图片路径，请重试");
        messages.put("gui.creating", "§a正在创建地图画: ");

        // Manager Messages
        messages.put("mgr.read_error", "无法读取图片文件: ");
        messages.put("mgr.read_unsupported", "无法读取图片，格式可能不支持: ");
        messages.put("mgr.size_too_large", "图片尺寸过大！最大允许: %dx%d");
        messages.put("mgr.mode_scale", "缩放");
        messages.put("mgr.mode_tile", "切分");
        messages.put("mgr.success", "成功创建地图画（%s模式）！共 %d 张地图");
        messages.put("mgr.error", "处理图片时出错: ");
        messages.put("mgr.info", "你持有 %d 张地图");

        // Version Checker Messages
        messages.put("version.update_header", "========== MapArt 版本更新可用 ==========");
        messages.put("version.update_current", "当前版本: v%s | 最新版本: %s");
        messages.put("version.update_download", "请前往下载最新版本: %s");
        messages.put("version.up_to_date", "[MapArt] 您的插件已是最新版本 (%s)");
        messages.put("version.snapshot", "[MapArt] 您的插件为测试版插件 (当前: v%s, 官方: %s)");

        // Web Upload Page Messages
        messages.put("web.title", "MapArt 图片上传");
        messages.put("web.subtitle", "上传图片到服务器生成地图画");
        messages.put("web.drop_text", "点击选择图片或拖拽到此处");
        messages.put("web.drop_hint", "支持 PNG / JPG / GIF / BMP / WEBP");
        messages.put("web.upload_btn", "上传图片");
        messages.put("web.uploading", "正在上传...");
        messages.put("web.footer", "MapArt Plugin &copy; 筑梦方舟网络科技工作室");
        messages.put("web.success", "上传成功！图片已保存，可以返回游戏使用 /mapart gui 查看");
        messages.put("web.invalid_token", "无效的令牌，请在游戏中执行 /mapart upload 获取新的链接");
        messages.put("web.invalid_type", "不支持的文件类型，请上传 PNG/JPG/GIF/BMP/WEBP 格式的图片");
        messages.put("web.size_exceeded", "图片尺寸超过限制 (最大 2048x2048)");
        messages.put("web.virus_detected", "文件验证失败，请确保上传的是有效的图片文件");
        messages.put("web.alert_invalid_type", "不支持的文件类型");
        messages.put("web.alert_file_too_large", "文件过大，最大 10MB");
    }
}
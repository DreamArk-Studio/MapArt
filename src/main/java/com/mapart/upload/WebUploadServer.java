package com.mapart.upload;

import com.mapart.MapArtPlugin;
import com.mapart.config.PluginConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class WebUploadServer {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");
    private static final byte[][] MAGIC_BYTES = {
        {(byte) 0x89, 0x50, 0x4E, 0x47},           // PNG
        {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},    // JPEG
        {0x47, 0x49, 0x46, 0x38},                   // GIF89a/GIF87a
        {0x42, 0x4D},                                // BMP
        {0x52, 0x49, 0x46, 0x46}                     // WEBP (RIFF)
    };

    private static final String[] EXTENSIONS = {"png", "jpg", "gif", "bmp", "webp"};

    private final MapArtPlugin plugin;
    private final UploadTokenManager tokenManager;
    private HttpServer server;

    public WebUploadServer(MapArtPlugin plugin, UploadTokenManager tokenManager) {
        this.plugin = plugin;
        this.tokenManager = tokenManager;
    }

    public void start() throws IOException {
        PluginConfig cfg = plugin.getPluginConfig();
        if (!cfg.isWebServerEnabled()) return;

        String host = cfg.getWebServerHost();
        int port = cfg.getWebServerPort();
        InetSocketAddress addr = new InetSocketAddress(host, port);
        server = HttpServer.create(addr, 0);
        server.createContext("/upload", this::handleUpload);
        server.setExecutor(null);
        server.start();
        plugin.getLogger().info("Web upload server started on " + host + ":" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web upload server stopped");
        }
    }

    private void handleUpload(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                handleGetUploadPage(exchange);
            } else if ("POST".equals(method)) {
                handlePostUpload(exchange);
            } else {
                sendResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Upload error: " + e.getMessage());
            try {
                sendResponse(exchange, 500, "Internal server error");
            } catch (IOException ignored) {
            }
        }
    }

    private void handleGetUploadPage(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String token = "";
        String message = "";
        String messageType = "";

        if (query != null) {
            Map<String, String> params = parseQuery(query);
            token = params.getOrDefault("token", "");
            String msg = params.getOrDefault("msg", "");
            if ("success".equals(msg)) {
                message = "上传成功！图片已保存，可以返回游戏使用 /mapart gui 查看";
                messageType = "success";
            } else if ("invalid".equals(msg)) {
                message = "无效的令牌，请在游戏中执行 /mapart upload 获取新的链接";
                messageType = "error";
            } else if ("type".equals(msg)) {
                message = "不支持的文件类型，请上传 PNG/JPG/GIF/BMP/WEBP 格式的图片";
                messageType = "error";
            } else if ("size".equals(msg)) {
                message = "图片尺寸超过限制 (最大 2048x2048)";
                messageType = "error";
            } else if ("virus".equals(msg)) {
                message = "文件验证失败，请确保上传的是有效的图片文件";
                messageType = "error";
            }
        }

        String html = buildUploadPage(token, message, messageType);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handlePostUpload(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            redirect(exchange, "?msg=invalid");
            return;
        }

        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            redirect(exchange, "?msg=invalid");
            return;
        }

        byte[] body;
        try (InputStream is = exchange.getRequestBody()) {
            body = is.readAllBytes();
        }

        Map<String, Part> parts = parseMultipart(body, boundary);
        Part filePart = parts.get("file");
        Part tokenPart = parts.get("token");

        if (filePart == null || tokenPart == null || filePart.data.length == 0) {
            redirect(exchange, "?msg=invalid");
            return;
        }

        String token = new String(tokenPart.data, StandardCharsets.UTF_8).trim();
        UploadTokenManager.TokenInfo tokenInfo = tokenManager.consumeToken(token);
        if (tokenInfo == null) {
            redirect(exchange, "?msg=invalid");
            return;
        }

        String fileName = filePart.fileName;
        if (fileName == null || fileName.isEmpty()) {
            fileName = "upload.png";
        }

        String ext = getExtension(fileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            redirect(exchange, "?msg=type");
            return;
        }

        if (!validateMagicBytes(filePart.data, ext)) {
            redirect(exchange, "?msg=virus");
            return;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(filePart.data)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                redirect(exchange, "?msg=virus");
                return;
            }
            int maxW = plugin.getPluginConfig().getMaxImageWidth();
            int maxH = plugin.getPluginConfig().getMaxImageHeight();
            if (img.getWidth() > maxW || img.getHeight() > maxH) {
                redirect(exchange, "?msg=size");
                return;
            }
        }

        String safeName = tokenInfo.playerName + "_" + System.currentTimeMillis() + "." + ext;
        File dest = new File(plugin.getPluginConfig().getImageDirectory(), safeName);
        Files.write(dest.toPath(), filePart.data);

        plugin.getLogger().info("Image uploaded by " + tokenInfo.playerName + ": " + safeName);

        redirect(exchange, "?token=" + token + "&msg=success");
    }

    private boolean validateMagicBytes(byte[] data, String ext) {
        if (data.length < 16) return false;
        int idx = getMagicIndex(ext);
        if (idx < 0) return false;
        byte[] magic = MAGIC_BYTES[idx];
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) return false;
        }
        return true;
    }

    private int getMagicIndex(String ext) {
        for (int i = 0; i < EXTENSIONS.length; i++) {
            if (EXTENSIONS[i].equals(ext)) return i;
        }
        return -1;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return "";
        return fileName.substring(dot + 1);
    }

    private String extractBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                return part.substring("boundary=".length());
            }
        }
        return null;
    }

    private Map<String, Part> parseMultipart(byte[] body, String boundary) {
        Map<String, Part> result = new HashMap<>();
        byte[] delim = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] endDelim = ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);

        int pos = 0;
        while (pos < body.length) {
            int start = indexOf(body, delim, pos);
            if (start < 0) break;
            int partStart = skipLine(body, start + delim.length);

            int next = indexOf(body, delim, partStart);
            if (next < 0) {
                int endIdx = indexOf(body, endDelim, partStart);
                if (endIdx < 0) break;
                next = endIdx;
            }

            int partEnd = next;
            if (partEnd > partStart) {
                Part part = parsePart(Arrays.copyOfRange(body, partStart, partEnd));
                if (part != null) {
                    result.put(part.name, part);
                }
            }
            pos = next;
        }
        return result;
    }

    private Part parsePart(byte[] data) {
        int headerEnd = indexOf(data, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), 0);
        if (headerEnd < 0) return null;

        String header = new String(data, 0, headerEnd, StandardCharsets.UTF_8);
        byte[] content = Arrays.copyOfRange(data, headerEnd + 4, data.length);

        String name = null;
        String fileName = null;

        for (String line : header.split("\r\n")) {
            if (line.toLowerCase().contains("content-disposition")) {
                for (String attr : line.split(";")) {
                    attr = attr.trim();
                    if (attr.startsWith("name=")) {
                        name = attr.substring(5).replace("\"", "");
                    } else if (attr.startsWith("filename=")) {
                        fileName = attr.substring(9).replace("\"", "");
                    }
                }
            }
        }

        if (name == null) return null;
        return new Part(name, fileName, content);
    }

    private int indexOf(byte[] data, byte[] pattern, int start) {
        outer:
        for (int i = start; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private int skipLine(byte[] data, int pos) {
        while (pos < data.length) {
            if (data[pos] == '\r' && pos + 1 < data.length && data[pos + 1] == '\n') {
                return pos + 2;
            }
            if (data[pos] == '\n') return pos + 1;
            pos++;
        }
        return pos;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                params.put(key, val);
            }
        }
        return params;
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void redirect(HttpExchange exchange, String query) throws IOException {
        String base = plugin.getPluginConfig().getWebPublicUrl() + "/upload";
        String location = base + query;
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    private String buildUploadPage(String token, String message, String messageType) {
        String baseUrl = plugin.getPluginConfig().getWebPublicUrl();
        String msgHtml = "";
        if (!message.isEmpty()) {
            String color = "success".equals(messageType) ? "#4CAF50" : "#f44336";
            msgHtml = "<div style='padding:16px;margin-bottom:20px;background:" + color + ";color:#fff;border-radius:8px;font-size:16px;'>"
                    + message + "</div>";
        }

        return "<!DOCTYPE html>"
                + "<html lang='zh-CN'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>MapArt 图片上传</title>"
                + "<style>"
                + "*{margin:0;padding:0;box-sizing:border-box;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;}"
                + "body{min-height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#1a1a2e,#16213e,#0f3460);color:#fff;}"
                + ".card{background:rgba(255,255,255,0.08);backdrop-filter:blur(20px);padding:40px;border-radius:20px;width:420px;max-width:90vw;box-shadow:0 8px 32px rgba(0,0,0,0.3);border:1px solid rgba(255,255,255,0.1);}"
                + "h1{text-align:center;font-size:28px;margin-bottom:8px;}"
                + ".subtitle{text-align:center;color:rgba(255,255,255,0.6);margin-bottom:30px;font-size:14px;}"
                + ".drop-zone{border:2px dashed rgba(255,255,255,0.3);border-radius:12px;padding:40px 20px;text-align:center;cursor:pointer;transition:all 0.3s;margin-bottom:20px;}"
                + ".drop-zone:hover,.drop-zone.dragover{border-color:#4CAF50;background:rgba(76,175,80,0.1);}"
                + ".drop-zone-icon{font-size:48px;margin-bottom:12px;opacity:0.6;}"
                + ".drop-zone-text{font-size:16px;color:rgba(255,255,255,0.8);}"
                + ".drop-zone-hint{font-size:13px;color:rgba(255,255,255,0.4);margin-top:8px;}"
                + ".file-info{display:none;padding:12px;background:rgba(255,255,255,0.05);border-radius:8px;margin-bottom:20px;font-size:14px;}"
                + ".file-info.show{display:flex;align-items:center;gap:12px;}"
                + ".file-info .name{flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}"
                + ".file-info .size{color:rgba(255,255,255,0.5);font-size:12px;}"
                + ".file-info .remove{cursor:pointer;color:#f44336;font-size:20px;line-height:1;}"
                + "button{width:100%;padding:14px;border:none;border-radius:10px;font-size:16px;cursor:pointer;transition:all 0.3s;font-weight:600;}"
                + "button.upload-btn{background:#4CAF50;color:#fff;}"
                + "button.upload-btn:hover{background:#45a049;}"
                + "button.upload-btn:disabled{background:rgba(76,175,80,0.3);cursor:not-allowed;}"
                + ".loading{display:none;text-align:center;margin-top:20px;}"
                + ".loading.show{display:block;}"
                + ".spinner{width:40px;height:40px;border:4px solid rgba(255,255,255,0.1);border-top-color:#4CAF50;border-radius:50%;animation:spin 0.8s linear infinite;margin:0 auto 10px;}"
                + "@keyframes spin{to{transform:rotate(360deg)}}"
                + ".footer{text-align:center;margin-top:24px;font-size:12px;color:rgba(255,255,255,0.3);}"
                + "input[type=file]{display:none;}"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<h1>🎨 MapArt</h1>"
                + "<p class='subtitle'>上传图片到服务器生成地图画</p>"
                + msgHtml
                + "<form id='uploadForm' enctype='multipart/form-data' method='post' action='" + baseUrl + "/upload'>"
                + "<input type='hidden' name='token' value='" + token + "'>"
                + "<div class='drop-zone' id='dropZone' onclick='document.getElementById(\"fileInput\").click()'>"
                + "<div class='drop-zone-icon'>📁</div>"
                + "<div class='drop-zone-text'>点击选择图片或拖拽到此处</div>"
                + "<div class='drop-zone-hint'>支持 PNG / JPG / GIF / BMP / WEBP</div>"
                + "</div>"
                + "<input type='file' id='fileInput' name='file' accept='image/png,image/jpeg,image/gif,image/bmp,image/webp'>"
                + "<div class='file-info' id='fileInfo'>"
                + "<span class='name' id='fileName'></span>"
                + "<span class='size' id='fileSize'></span>"
                + "<span class='remove' onclick='removeFile()'>×</span>"
                + "</div>"
                + "<button type='submit' class='upload-btn' id='submitBtn' disabled>上传图片</button>"
                + "</form>"
                + "<div class='loading' id='loading'>"
                + "<div class='spinner'></div>"
                + "<span>正在上传...</span>"
                + "</div>"
                + "<div class='footer'>MapArt Plugin &copy; 筑梦方舟网络科技工作室</div>"
                + "</div>"
                + "<script>"
                + "const dropZone=document.getElementById('dropZone');const fileInput=document.getElementById('fileInput');"
                + "const fileInfo=document.getElementById('fileInfo');const fileName=document.getElementById('fileName');"
                + "const fileSize=document.getElementById('fileSize');const submitBtn=document.getElementById('submitBtn');"
                + "const form=document.getElementById('uploadForm');const loading=document.getElementById('loading');"
                + "let selectedFile=null;"
                + "fileInput.addEventListener('change',function(){selectFile(this.files[0])});"
                + "dropZone.addEventListener('dragover',function(e){e.preventDefault();this.classList.add('dragover')});"
                + "dropZone.addEventListener('dragleave',function(){this.classList.remove('dragover')});"
                + "dropZone.addEventListener('drop',function(e){e.preventDefault();this.classList.remove('dragover');const f=e.dataTransfer.files[0];if(f){selectFile(f);fileInput.files=e.dataTransfer.files;}});"
                + "function selectFile(f){if(!f){return}selectedFile=f;const validTypes=['image/png','image/jpeg','image/gif','image/bmp','image/webp'];"
                + "if(!validTypes.includes(f.type)){alert('不支持的文件类型');return;}"
                + "if(f.size>10*1024*1024){alert('文件过大，最大 10MB');return;}"
                + "fileName.textContent=f.name;const sizes=['B','KB','MB'];let i=0;let sz=f.size;while(sz>=1024&&i<2){sz/=1024;i++}fileSize.textContent=sz.toFixed(1)+sizes[i];"
                + "fileInfo.classList.add('show');submitBtn.disabled=false;}"
                + "function removeFile(){selectedFile=null;fileInput.value='';fileInfo.classList.remove('show');submitBtn.disabled=true;}"
                + "form.addEventListener('submit',function(){loading.classList.add('show');submitBtn.disabled=true;});"
                + "</script></body></html>";
    }

    private static class Part {
        final String name;
        final String fileName;
        final byte[] data;

        Part(String name, String fileName, byte[] data) {
            this.name = name;
            this.fileName = fileName;
            this.data = data;
        }
    }
}
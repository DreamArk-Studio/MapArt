# MapArt - Minecraft Map Art Plugin

A map art plugin for Purpur/Paper servers that allows players to convert custom images into in-game map paintings.

## Features

- Supports PNG, JPG, GIF, BMP image formats
- Auto-scales and crops images to fit map dimensions
- Large images automatically split into multiple maps
- Async processing, no server lag
- Folia architecture support

## Installation

1. Place `MapArt-1.1.2.jar` into your server's `plugins` directory
2. Restart the server
3. Players use `/mapart upload` to upload images (saved to `plugins/MapArt/images/<playerUUID>/`)
4. Or manually place images into `plugins/MapArt/images/` directory

## Usage

| Command | Description |
|---------|-------------|
| `/mapart` | Open GUI (shows current player images) |
| `/mapart gui` | Open GUI |
| `/mapart upload` | Get a web upload link (player-specific) |
| `/mapart apply <image file> [scale|tile]` | Convert image to map art (scale: fit single map, tile: split into grid) |
| `/mapart clear` | Clear all map art |
| `/mapart info` | View number of maps you hold |
| `/mapart list` | List current player's available image files |

**Example:**
```
/mapart apply myimage.png           # Scale to single map (default)
/mapart apply myimage.png tile      # Split into multiple maps for large image
```

### Two Modes

| Mode | Command | Description |
|------|---------|-------------|
| **Scale** | `/mapart apply <image>` | Default. Scales image to 128x128, outputs 1 map |
| **Tile** | `/mapart apply <image> tile` | No scaling. Splits image into 128x128 tiles, outputs multiple maps for mosaic |

## Recommended Image Size

### Standard Single Map
- **Recommended size: 128x128 pixels**
- Exactly matches one map's 128x128 pixel display area, one-to-one pixel mapping with no scaling distortion

### Size Comparison

| Image Size | Map Count | Description |
|------------|-----------|-------------|
| **128x128** | **1 map** | **Recommended, pixel-perfect mapping** |
| 256x256 | 1 map | Auto-scaled down to 128x128 |
| 512x512 | 1 map | Auto-scaled down to 128x128 |
| 1024x1024 | 1 map | Auto-scaled down to 128x128 |
| 2048x2048 | 1 map | Auto-scaled down to 128x128 |
| 256x128 | 2 maps | Horizontal split, auto-divided |
| 512x256 | 8 maps | 4x2 map grid |

### Important Notes

- Minecraft maps only have a **128x128 pixel** display area
- Using a 128x128 image gives you precise pixel-by-pixel control, WYSIWYG
- Images larger than 128x128 are automatically split into multiple maps
- Images smaller than 128x128 are auto-scaled to fill

### Color Limitations

- Minecraft maps only have **128 available colors**
- Use images with high color contrast for best results
- Avoid images with excessive gradients
- Pixel art style images work best

## Configuration

`plugins/MapArt/config.yml`:

### Web Upload Server

Controls the built-in web upload server.

| Config | Type | Default | Description |
|--------|------|---------|-------------|
| `web-server.enabled` | boolean | `true` | Enable/disable the web upload server. When off, players must place images manually |
| `web-server.host` | string | `0.0.0.0` | Listen address. `0.0.0.0` = all interfaces, `127.0.0.1` = local only |
| `web-server.port` | int | `8080` | Listen port. Ensure firewall allows traffic |
| `web-server.public-url` | string | `http://127.0.0.1:8080` | URL opened by players. Use `http://127.0.0.1:port` for local testing, server public IP for remote |

> **Note**: `public-url` must use `http://` (not `https://`) unless you have a reverse proxy providing HTTPS.

### Image Limits

| Config | Type | Default | Description |
|--------|------|---------|-------------|
| `max-image-width` | int | `2048` | Maximum allowed image width (pixels) |
| `max-image-height` | int | `2048` | Maximum allowed image height (pixels) |

Images exceeding these limits will be rejected on upload.

### Map Settings

| Config | Type | Default | Description |
|--------|------|---------|-------------|
| `map-size` | int | `128` | Pixel size of a single map. Standard Minecraft maps are `128x128` |

### Performance Settings

| Config | Type | Default | Description |
|--------|------|---------|-------------|
| `async-processing` | boolean | `true` | Process images asynchronously (recommended) |
| `max-concurrent-tasks` | int | `4` | Max concurrent processing tasks |

### Full Config Example

```yaml
web-server:
  enabled: true
  host: 0.0.0.0
  port: 8080
  public-url: "http://127.0.0.1:8080"

max-image-width: 2048
max-image-height: 2048
map-size: 128
async-processing: true
max-concurrent-tasks: 4
```

### Public Server Example

If your server's public IP is `1.2.3.4` and port is `8080`:

```yaml
web-server:
  enabled: true
  host: 0.0.0.0
  port: 8080
  public-url: "http://1.2.3.4:8080"
```

Then set up port forwarding: external `8080` → internal server `8080`.

### Player image isolation
- Uploaded files are saved under: `plugins/MapArt/images/<playerUUID>/`
- Each player can only see and use their own uploaded images

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `mapart.use` | Use map art commands | OP |
| `mapart.admin` | Admin permissions | OP |

## Building

```bash
mvn clean package
```

Build output is located at `target/MapArt-1.1.2.jar`

## Dependencies

- Purpur 1.21+ or Paper 1.21+
- Java 21+

## Telemetry

This plugin collects anonymous usage data to help improve the project. A unique UUID is generated on first launch and stored in `DreamArk/server-uuid.txt`. Heartbeat data is sent every 60 seconds to our telemetry server. No personal or server content data is collected.

By using this plugin, you agree to our [Privacy Policy](https://www.dreamark.club/page.php?slug=privacy) and [Terms of Service](https://www.dreamark.club/page.php?slug=terms).

## Credits

Developed by [DreamArk Studio](https://www.dreamark.club/)

package com.mapart.renderer;

import org.bukkit.map.MapPalette;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 将图片转换为 Minecraft 地图颜色
 */
public class MapArtRenderer {

    public enum Mode {
        SCALE,
        TILE
    }

    /**
     * 将 BufferedImage 转换为 Minecraft 地图字节数组
     * 
     * @param image 输入图片
     * @param mapSize 地图尺寸 (默认 128)
     * @return 地图颜色字节数组
     */
    public byte[] renderToMap(BufferedImage image, int mapSize) {
        byte[] result = new byte[mapSize * mapSize];
        
        BufferedImage scaled = resizeImage(image, mapSize, mapSize);
        
        for (int y = 0; y < mapSize; y++) {
            for (int x = 0; x < mapSize; x++) {
                int rgb = scaled.getRGB(x, y);
                Color color = new Color(rgb, true);
                byte mapColor = MapPalette.matchColor(color);
                result[y * mapSize + x] = mapColor;
            }
        }
        
        return result;
    }

    /**
     * 根据模式渲染图片
     * 
     * @param image 输入图片
     * @param mapSize 地图尺寸
     * @param mode SCALE: 缩放到单张地图, TILE: 切分为多张地图
     * @return 地图字节数组
     */
    public byte[][] renderImage(BufferedImage image, int mapSize, Mode mode) {
        if (mode == Mode.TILE) {
            return renderTileImage(image, mapSize);
        }
        BufferedImage scaled = resizeImage(image, mapSize, mapSize);
        return new byte[][]{renderToMap(scaled, mapSize)};
    }

    /**
     * 将图片按 mapSize 切分为多个地图块（不缩放）
     */
    private byte[][] renderTileImage(BufferedImage image, int mapSize) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        
        int mapsX = (int) Math.ceil((double) imageWidth / mapSize);
        int mapsY = (int) Math.ceil((double) imageHeight / mapSize);
        
        byte[][][] result = new byte[mapsY][mapsX][mapSize * mapSize];
        
        for (int mapY = 0; mapY < mapsY; mapY++) {
            for (int mapX = 0; mapX < mapsX; mapX++) {
                int x = mapX * mapSize;
                int y = mapY * mapSize;
                int width = Math.min(mapSize, imageWidth - x);
                int height = Math.min(mapSize, imageHeight - y);
                
                BufferedImage subImage = image.getSubimage(x, y, width, height);
                
                if (width < mapSize || height < mapSize) {
                    BufferedImage padded = new BufferedImage(mapSize, mapSize, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = padded.createGraphics();
                    g.drawImage(subImage, 0, 0, null);
                    g.dispose();
                    subImage = padded;
                }
                
                result[mapY][mapX] = renderToMap(subImage, mapSize);
            }
        }
        
        int totalMaps = mapsX * mapsY;
        byte[][] flatResult = new byte[totalMaps][];
        int index = 0;
        for (int mapY = 0; mapY < mapsY; mapY++) {
            for (int mapX = 0; mapX < mapsX; mapX++) {
                flatResult[index++] = result[mapY][mapX];
            }
        }
        
        return flatResult;
    }

    /**
     * 缩放图片
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();
        return outputImage;
    }

    /**
     * 获取需要的总地图数量
     * 
     * @param image 输入图片
     * @param mapSize 地图尺寸
     * @param mode SCALE: 始终 1 张, TILE: 按实际切分计算
     */
    public int getRequiredMapCount(BufferedImage image, int mapSize, Mode mode) {
        if (mode == Mode.TILE) {
            int mapsX = (int) Math.ceil((double) image.getWidth() / mapSize);
            int mapsY = (int) Math.ceil((double) image.getHeight() / mapSize);
            return mapsX * mapsY;
        }
        return 1;
    }
}

package com.mapart.renderer;

import org.bukkit.map.MapPalette;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 将图片转换为 Minecraft 地图颜色
 */
public class MapArtRenderer {

    /**
     * 将 BufferedImage 转换为 Minecraft 地图字节数组
     * 
     * @param image 输入图片
     * @param mapSize 地图尺寸 (默认 128)
     * @return 地图颜色字节数组
     */
    public byte[] renderToMap(BufferedImage image, int mapSize) {
        byte[] result = new byte[mapSize * mapSize];
        
        // 缩放图片到地图尺寸
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
     * 将大图片分割成多个地图块
     * 
     * @param image 输入图片
     * @param mapSize 每个地图块的尺寸
     * @return 二维数组，每个元素是一个地图块的字节数组
     */
    public byte[][] renderLargeImage(BufferedImage image, int mapSize) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        
        // 计算需要多少张地图
        int mapsX = (int) Math.ceil((double) imageWidth / mapSize);
        int mapsY = (int) Math.ceil((double) imageHeight / mapSize);
        
        byte[][][] result = new byte[mapsY][mapsX][mapSize * mapSize];
        
        for (int mapY = 0; mapY < mapsY; mapY++) {
            for (int mapX = 0; mapX < mapsX; mapX++) {
                // 提取当前地图块的区域
                int x = mapX * mapSize;
                int y = mapY * mapSize;
                int width = Math.min(mapSize, imageWidth - x);
                int height = Math.min(mapSize, imageHeight - y);
                
                BufferedImage subImage = image.getSubimage(x, y, width, height);
                
                // 如果不足 mapSize，需要填充
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
        
        // 展平为一维数组
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
     * 获取图片需要的地图数量
     */
    public int getRequiredMapCount(BufferedImage image, int mapSize) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int mapsX = (int) Math.ceil((double) imageWidth / mapSize);
        int mapsY = (int) Math.ceil((double) imageHeight / mapSize);
        return mapsX * mapsY;
    }
}

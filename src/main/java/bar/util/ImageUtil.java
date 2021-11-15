package bar.util;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public abstract class ImageUtil {


    public static BufferedImage overlayColor(BufferedImage image, Color color, float amount) {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, amount));
        g.setColor(color);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
        return output;
    }

    public static BufferedImage saturateImage(BufferedImage image, float saturation) {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                float[] hsb = Color.RGBtoHSB(getRed(rgb), getGreen(rgb), getBlue(rgb), null);
                float hue = hsb[0];
                float brightness = hsb[2];
                int rgb2 = Color.HSBtoRGB(hue, saturation, brightness);
                output.setRGB(x, y, rgb2);
            }
        }
        return output;
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return output;
    }

    public static Color averageColor(BufferedImage bi) {
        int x1 = bi.getWidth();
        int y1 = bi.getHeight();
        long sumr = 0, sumg = 0, sumb = 0;
        int skipper = 0;
        for (int x = 0; x < x1; x++) {
            for (int y = 0; y < y1; y++) {
                if (++skipper > 1) {
                    skipper = 0;
                    continue;
                }
                Color pixel = new Color(bi.getRGB(x, y));
                sumr += pixel.getRed();
                sumg += pixel.getGreen();
                sumb += pixel.getBlue();
            }
        }
        int num = bi.getWidth() * bi.getHeight();
        return new Color((int) (sumr / toF(num)), (int) (sumg / toF(num)), (int) (sumb / toF(num)));
    }

    public static int getAlpha(int rgb) {
        return (rgb >> 24) & 0xFF;
    }

    public static int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int getBlue(int rgb) {
        return rgb & 0xFF;
    }

    private static float toF(int i) {
        return Float.parseFloat("" + i);
    }
}

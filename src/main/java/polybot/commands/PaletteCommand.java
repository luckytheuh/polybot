package polybot.commands;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PaletteCommand {


    Color[] getPaletteFromImage(BufferedImage image) {
        //scale image down to like 16x16

        BufferedImage scaled = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.drawImage(image, 0, 0, 16, 16, null);
        g2d.dispose();

        //idk, like get the colors that show up the most

return null;
    }


    List<Color> getColors(BufferedImage image) {
        List<Color> colors = new ArrayList<>();

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                colors.add(new Color(image.getRGB(x, y)));
            }
        }

        return colors;
    }
/*
    String findBiggestColorRange() {
        long[] minMax = new long[]{Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};

        List<Color> colors = getColors(new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB));
        colors.forEach(color -> {
            minMax[0] = Math.min(minMax[0], color.getRed());
            minMax[1] = Math.min(minMax[1], color.getBlue());
            minMax[2] = Math.min(minMax[2], color.getGreen());

            minMax[3] = Math.max(minMax[3], color.getRed());
            minMax[4] = Math.max(minMax[4], color.getBlue());
            minMax[5] = Math.max(minMax[5], color.getGreen());
        });


        long rRange = minMax[0] - minMax[3], gRange = minMax[1] - minMax[4], bRange = minMax[2] - minMax[5];
        long biggestRange = Math.max(Math.max(rRange, gRange), Math.max(gRange, bRange));

        if (biggestRange == rRange) return "r";
        else if (biggestRange == gRange) return "g";
        else return "b";
    }*/

}

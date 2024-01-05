package de.theredend2000.advancedegghunt.util;


import org.bukkit.Bukkit;

import java.awt.*;
import java.awt.image.BufferedImage;

public class StartupMessages {

    public void sendMessages(){
        int width = 80;
        int height = 10;
        int fontSize = 12;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setFont(new Font("Monospaced", Font.PLAIN, fontSize)); // Verwende Monospaced

        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawString("Advanced", 0, fontSize);
        graphics.drawString("EggHunt", 0, 2 * fontSize);

        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < width; x++) {
                sb.append(image.getRGB(x, y) == -16777216 ? " " : "#");
            }

            if (sb.toString().trim().isEmpty()) {
                continue;
            }

            System.out.print("\u001B[33m");
            System.out.print(sb);
            System.out.print("\u001B[0m\n");
        }
    }
}

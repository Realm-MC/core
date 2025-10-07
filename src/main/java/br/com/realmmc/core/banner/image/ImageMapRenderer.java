package br.com.realmmc.core.banner.image;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;

public class ImageMapRenderer extends MapRenderer {

    private final BufferedImage image;
    private boolean rendered = false;

    public ImageMapRenderer(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) {
            return;
        }

        BufferedImage scaledImage = MapPalette.resizeImage(this.image);

        canvas.drawImage(0, 0, scaledImage);

        rendered = true;
    }
}
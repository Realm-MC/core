package br.com.realmmc.core.banner.image;

import br.com.realmmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;

public class ImageMapConverter {

    private final Main plugin;

    public ImageMapConverter(Main plugin) {
        this.plugin = plugin;
    }

    public int[][] convertImageToMapIds(String imageFileName, int mapWidth, int mapHeight) {
        try (InputStream inputStream = plugin.getResource("images/" + imageFileName)) {
            if (inputStream == null) {
                plugin.getLogger().warning("Não foi possível encontrar a imagem '" + imageFileName + "' dentro do plugin. Verifica a pasta 'resources/images'.");
                return null;
            }

            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                plugin.getLogger().warning("Não foi possível ler a imagem do ficheiro: " + imageFileName);
                return null;
            }

            int subImageWidth = 128;
            int subImageHeight = 128;

            BufferedImage resizedImage = new BufferedImage(subImageWidth * mapWidth, subImageHeight * mapHeight, BufferedImage.TYPE_INT_ARGB);
            resizedImage.getGraphics().drawImage(originalImage, 0, 0, subImageWidth * mapWidth, subImageHeight * mapHeight, null);

            int[][] mapIds = new int[mapHeight][mapWidth];
            World defaultWorld = Bukkit.getWorlds().get(0);

            for (int y = 0; y < mapHeight; y++) {
                for (int x = 0; x < mapWidth; x++) {
                    BufferedImage subImage = resizedImage.getSubimage(x * subImageWidth, y * subImageHeight, subImageWidth, subImageHeight);
                    MapView mapView = Bukkit.createMap(defaultWorld);
                    mapView.getRenderers().forEach(mapView::removeRenderer);
                    mapView.addRenderer(new ImageMapRenderer(subImage));
                    mapIds[y][x] = mapView.getId();
                }
            }
            plugin.getLogger().info("Imagem '" + imageFileName + "' convertida para " + (mapWidth * mapHeight) + " mapas.");
            return mapIds;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao converter imagem para mapas: " + imageFileName, e);
            return null;
        }
    }
}
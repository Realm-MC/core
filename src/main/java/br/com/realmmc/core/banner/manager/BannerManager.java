package br.com.realmmc.core.banner.manager;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.banner.action.ActionType;
import br.com.realmmc.core.banner.action.BannerAction;
import br.com.realmmc.core.banner.action.ClickType;
import br.com.realmmc.core.banner.model.Banner;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BannerManager {

    private final Main plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Map<String, Banner> bannerCache = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public BannerManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "banners.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Não foi possível criar o banners.yml", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void loadBanners() {
        bannerCache.clear();
        this.config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("banners");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            try {
                String path = "banners." + id;
                String worldName = config.getString(path + ".world");
                if (worldName == null || Bukkit.getWorld(worldName) == null) {
                    plugin.getLogger().warning("Mundo '" + worldName + "' para o banner '" + id + "' não encontrado.");
                    continue;
                }

                Location loc = new Location(Bukkit.getWorld(worldName), config.getDouble(path + ".top-left-location.x"), config.getDouble(path + ".top-left-location.y"), config.getDouble(path + ".top-left-location.z"));
                BlockFace face = BlockFace.valueOf(config.getString(path + ".facing", "NORTH").toUpperCase());
                String imageFile = config.getString(path + ".image-file");
                int width = config.getInt(path + ".width");
                int height = config.getInt(path + ".height");

                Type type = new TypeToken<int[][]>(){}.getType();
                int[][] mapIds = gson.fromJson(config.getString(path + ".map-ids"), type);

                if (mapIds == null) {
                    throw new JsonSyntaxException("O campo 'map-ids' está nulo ou inválido no banner '" + id + "'");
                }

                Banner banner = new Banner(id, imageFile, width, height, mapIds, loc, face);

                ConfigurationSection actionsSection = config.getConfigurationSection(path + ".actions");
                if (actionsSection != null) {
                    for (String clickTypeStr : actionsSection.getKeys(false)) {
                        ClickType clickType = ClickType.fromString(clickTypeStr);
                        ActionType actionType = ActionType.fromString(actionsSection.getString(clickTypeStr + ".type"));
                        String value = actionsSection.getString(clickTypeStr + ".value");
                        if (clickType != null && actionType != null && value != null) {
                            banner.addAction(clickType, new BannerAction(actionType, value));
                        }
                    }
                }
                bannerCache.put(id.toLowerCase(), banner);
            } catch (Exception e) {
                plugin.getLogger().severe("Falha ao carregar o banner '" + id + "' do banners.yml. Verifica a formatação do ficheiro.");
                e.printStackTrace();
            }
        }
        plugin.getLogger().info(bannerCache.size() + " banners (imagens completas) foram carregados.");
    }

    public void saveBanners() {
        config.set("banners", null);
        for (Banner banner : bannerCache.values()) {
            String path = "banners." + banner.getId();
            config.set(path + ".world", banner.getTopLeftLocation().getWorld().getName());
            config.set(path + ".top-left-location.x", banner.getTopLeftLocation().getX());
            config.set(path + ".top-left-location.y", banner.getTopLeftLocation().getY());
            config.set(path + ".top-left-location.z", banner.getTopLeftLocation().getZ());
            config.set(path + ".facing", banner.getFacing().name());
            config.set(path + ".image-file", banner.getImageFile());
            config.set(path + ".width", banner.getWidth());
            config.set(path + ".height", banner.getHeight());
            config.set(path + ".map-ids", gson.toJson(banner.getMapIds()));

            for (Map.Entry<ClickType, BannerAction> entry : banner.getActions().entrySet()) {
                String actionPath = path + ".actions." + entry.getKey().name();
                config.set(actionPath + ".type", entry.getValue().type().name());
                config.set(actionPath + ".value", entry.getValue().value());
            }
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar o banners.yml", e);
        }
    }

    public void createAndSaveBanner(String id, String imageFile, int width, int height, int[][] mapIds, Location topLeftLocation, BlockFace facing) {
        Banner banner = new Banner(id, imageFile, width, height, mapIds, topLeftLocation, facing);
        bannerCache.put(id.toLowerCase(), banner);
        saveBanners();
    }

    public void spawnAllBanners() {
        plugin.getLogger().info("Criando entidades dos banners interativos...");
        for (Banner banner : bannerCache.values()) {
            Location topLeft = banner.getTopLeftLocation();
            if (topLeft.getWorld() == null) {
                plugin.getLogger().warning("Mundo do banner '" + banner.getId() + "' não encontrado. O banner não será criado.");
                continue;
            }
            Location blockLoc = new Location(topLeft.getWorld(), topLeft.getBlockX(), topLeft.getBlockY(), topLeft.getBlockZ());
            if (!blockLoc.getChunk().isLoaded()) {
                blockLoc.getChunk().load();
            }

            BlockFace facing = banner.getFacing();
            BlockFace rightDir, downDir;
            downDir = BlockFace.DOWN;

            switch (facing) {
                case NORTH: rightDir = BlockFace.WEST; break;
                case SOUTH: rightDir = BlockFace.EAST; break;
                case WEST: rightDir = BlockFace.SOUTH; break;
                case EAST: rightDir = BlockFace.NORTH; break;
                default: continue;
            }

            for (int y = 0; y < banner.getHeight(); y++) {
                for (int x = 0; x < banner.getWidth(); x++) {
                    Location partLocation = topLeft.getBlock().getRelative(rightDir, x).getRelative(downDir, y).getLocation();

                    boolean alreadyExists = false;
                    for (Entity entity : partLocation.getChunk().getEntities()) {
                        if (entity instanceof ItemFrame && entity.getLocation().getBlock().equals(partLocation.getBlock())) {
                            if (entity.hasMetadata("realm_banner_id") && entity.getMetadata("realm_banner_id").get(0).asString().equals(banner.getId())) {
                                alreadyExists = true;
                                break;
                            }
                        }
                    }

                    if (alreadyExists) continue;

                    final int currentX = x;
                    final int currentY = y;

                    partLocation.getWorld().spawn(partLocation, ItemFrame.class, frame -> {
                        frame.setFacingDirection(facing, true);
                        frame.setVisible(false);
                        frame.setInvulnerable(true);
                        frame.setFixed(true);

                        int mapId = banner.getMapIds()[currentY][currentX];
                        MapView mapView = Bukkit.getMap(mapId);

                        if (mapView == null) {
                            frame.setItem(new ItemStack(Material.PAPER));
                        } else {
                            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                            MapMeta meta = (MapMeta) mapItem.getItemMeta();
                            meta.setMapView(mapView);
                            mapItem.setItemMeta(meta);
                            frame.setItem(mapItem);
                        }
                        frame.setMetadata("realm_banner_id", new FixedMetadataValue(plugin, banner.getId()));
                    });
                }
            }
        }
        plugin.getLogger().info("Entidades dos banners criadas.");
    }

    public void despawnAllBanners() {
        plugin.getLogger().info("Removendo entidades dos banners...");
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType() == EntityType.ITEM_FRAME && entity.hasMetadata("realm_banner_id")) {
                    entity.remove();
                }
            }
        }
        plugin.getLogger().info("Entidades dos banners removidas.");
    }

    public void deleteBanner(String id) {
        bannerCache.remove(id.toLowerCase());
        saveBanners();
        despawnAllBanners();
        spawnAllBanners();
    }

    public Optional<Banner> getBanner(String id) {
        return Optional.ofNullable(bannerCache.get(id.toLowerCase()));
    }

    public Collection<Banner> getAllBanners() {
        return bannerCache.values();
    }
}
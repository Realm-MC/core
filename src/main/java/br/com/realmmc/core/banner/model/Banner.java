package br.com.realmmc.core.banner.model;

import br.com.realmmc.core.banner.action.BannerAction;
import br.com.realmmc.core.banner.action.ClickType;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Banner {

    private final String id;
    private final String imageFile;
    private final int width;
    private final int height;
    private final int[][] mapIds;
    private Location topLeftLocation;
    private BlockFace facing;
    private final Map<ClickType, BannerAction> actions;

    public Banner(String id, String imageFile, int width, int height, int[][] mapIds, Location topLeftLocation, BlockFace facing) {
        this.id = id;
        this.imageFile = imageFile;
        this.width = width;
        this.height = height;
        this.mapIds = mapIds;
        this.topLeftLocation = topLeftLocation;
        this.facing = facing;
        this.actions = new ConcurrentHashMap<>();
    }

    public void addAction(ClickType clickType, BannerAction action) {
        this.actions.put(clickType, action);
    }

    public void removeAction(ClickType clickType) {
        this.actions.remove(clickType);
    }

    public Optional<BannerAction> getAction(ClickType clickType) {
        return Optional.ofNullable(actions.getOrDefault(clickType, actions.get(ClickType.ANY)));
    }

    public String getId() { return id; }
    public String getImageFile() { return imageFile; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int[][] getMapIds() { return mapIds; }
    public Location getTopLeftLocation() { return topLeftLocation; }
    public BlockFace getFacing() { return facing; }
    public Map<ClickType, BannerAction> getActions() { return actions; }

    public void setTopLeftLocation(Location topLeftLocation) { this.topLeftLocation = topLeftLocation; }
    public void setFacing(BlockFace facing) { this.facing = facing; }
}
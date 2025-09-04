package br.com.realmmc.core.npc;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;

public class NPC {

    public record ClickAlert(String mode, String text) {}

    private final String id;
    private String displayName;
    private Location location;
    private String skinValue;
    private String skinSignature;
    private String skinUrl;
    private String actionType;
    private List<String> actionValues;
    private final String server;
    private ClickAlert clickAlert;

    public NPC(String id, String displayName, Location location, String skinValue, String skinSignature, String skinUrl, String actionType, List<String> actionValues, String server, ClickAlert clickAlert) {
        this.id = id;
        this.displayName = displayName;
        this.location = location;
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
        this.skinUrl = skinUrl;
        this.actionType = actionType;
        this.actionValues = actionValues;
        this.server = server;
        this.clickAlert = clickAlert;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Location getLocation() { return location; }
    public String getSkinValue() { return skinValue; }
    public String getSkinSignature() { return skinSignature; }
    public String getSkinUrl() { return skinUrl; }
    public String getActionType() { return actionType; }
    public List<String> getActionValues() { return actionValues; }
    public String getServer() { return server; }
    public Optional<ClickAlert> getClickAlert() { return Optional.ofNullable(clickAlert); }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setLocation(Location location) { this.location = location; }
    public void setSkin(String value, String signature) {
        this.skinValue = value;
        this.skinSignature = signature;
    }
    public void setAction(String actionType, List<String> actionValues) {
        this.actionType = actionType;
        this.actionValues = actionValues;
    }
    public void setClickAlert(ClickAlert clickAlert) { this.clickAlert = clickAlert; }

    public Document toDocument() {
        Document doc = new Document("_id", this.id)
                .append("displayName", this.displayName)
                .append("skinUrl", this.skinUrl)
                .append("skinValue", this.skinValue)
                .append("skinSignature", this.skinSignature)
                .append("actionType", this.actionType)
                .append("actionValues", this.actionValues)
                .append("server", this.server);

        if (this.clickAlert != null) {
            doc.append("clickAlert", new Document("mode", this.clickAlert.mode()).append("text", this.clickAlert.text()));
        }

        if (this.location != null && this.location.getWorld() != null) {
            doc.append("location", new Document()
                    .append("world", this.location.getWorld().getName())
                    .append("x", this.location.getX())
                    .append("y", this.location.getY())
                    .append("z", this.location.getZ())
                    .append("yaw", (double) this.location.getYaw())
                    .append("pitch", (double) this.location.getPitch()));
        }
        return doc;
    }

    public static NPC fromDocument(Document doc) {
        Location location = null;
        if (doc.containsKey("location")) {
            Document locDoc = doc.get("location", Document.class);
            if (Bukkit.getWorld(locDoc.getString("world")) != null) {
                location = new Location(
                        Bukkit.getWorld(locDoc.getString("world")),
                        locDoc.get("x", Number.class).doubleValue(),
                        locDoc.get("y", Number.class).doubleValue(),
                        locDoc.get("z", Number.class).doubleValue(),
                        locDoc.get("yaw", Number.class).floatValue(),
                        locDoc.get("pitch", Number.class).floatValue()
                );
            }
        }

        ClickAlert clickAlert = null;
        if (doc.containsKey("clickAlert")) {
            Document alertDoc = doc.get("clickAlert", Document.class);
            clickAlert = new ClickAlert(alertDoc.getString("mode"), alertDoc.getString("text"));
        }

        return new NPC(
                doc.getString("_id"),
                doc.getString("displayName"),
                location,
                doc.getString("skinValue"),
                doc.getString("skinSignature"),
                doc.getString("skinUrl"),
                doc.getString("actionType"),
                doc.getList("actionValues", String.class),
                doc.getString("server"),
                clickAlert
        );
    }
}
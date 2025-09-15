package br.com.realmmc.core.npc;

import br.com.realmmc.core.npc.skin.Skin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bson.Document;
import org.bukkit.Location;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa uma instância de um NPC, contendo todas as suas propriedades,
 * incluindo aparência, localização e ações interativas.
 */
public class NPC {

    private final String id;
    private final int entityId;
    private final UUID uuid;
    private String name;
    private String displayName;
    private Location location;
    private Skin skin;
    private String skinUsername;
    private String skinUrl;
    private String actionType;
    private List<String> actionValues;
    private final String server;
    private boolean lookAtPlayer;
    private ClickAlert clickAlert;
    private final Set<UUID> viewers;
    private boolean isGlobal;

    public record ClickAlert(String mode, String text) {
        public Document toDocument() {
            return new Document("mode", mode).append("text", text);
        }

        public static ClickAlert fromDocument(Document doc) {
            if (doc == null) return null;
            return new ClickAlert(doc.getString("mode"), doc.getString("text"));
        }
    }

    public NPC(String id, int entityId, UUID uuid, String name, String displayName, Location location, Skin skin, String skinUsername, String skinUrl, String actionType, List<String> actionValues, String server, boolean lookAtPlayer, ClickAlert clickAlert) {
        this.id = id;
        this.entityId = entityId;
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
        this.location = location;
        this.skin = skin;
        this.skinUsername = skinUsername;
        this.skinUrl = skinUrl;
        this.actionType = actionType;
        this.actionValues = actionValues;
        this.server = server;
        this.lookAtPlayer = lookAtPlayer;
        this.clickAlert = clickAlert;
        this.viewers = ConcurrentHashMap.newKeySet();
        this.isGlobal = false;
    }

    public static NPC fromDocument(Document doc, int entityId) {
        Location location = doc.containsKey("location") ? Location.deserialize(doc.get("location", Document.class)) : null;
        Skin skin = doc.containsKey("skinValue") ? new Skin(doc.getString("skinValue"), doc.getString("skinSignature")) : null;

        String displayName = doc.getString("displayName");

        // LÓGICA DE SEGURANÇA ADICIONADA AQUI
        String name = doc.getString("name");
        if (name == null || name.isBlank()) {
            // Se o campo 'name' não existir, usa o 'displayName', remove as cores e limita a 16 caracteres.
            name = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName).content();
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
        }

        return new NPC(
                doc.getString("_id"),
                entityId,
                UUID.randomUUID(),
                name,
                displayName,
                location,
                skin,
                doc.getString("skinUsername"),
                doc.getString("skinUrl"),
                doc.getString("actionType"),
                doc.getList("actionValues", String.class, new ArrayList<>()),
                doc.getString("server"),
                doc.getBoolean("lookAtPlayer", true),
                ClickAlert.fromDocument(doc.get("clickAlert", Document.class))
        );
    }

    public Document toDocument() {
        Document doc = new Document("_id", this.id)
                .append("name", this.name)
                .append("displayName", this.displayName)
                .append("skinUsername", this.skinUsername)
                .append("skinUrl", this.skinUrl)
                .append("actionType", this.actionType)
                .append("actionValues", this.actionValues)
                .append("server", this.server)
                .append("lookAtPlayer", this.lookAtPlayer);

        if (this.skin != null) {
            doc.append("skinValue", this.skin.value()).append("skinSignature", this.skin.signature());
        }
        if (this.clickAlert != null) {
            doc.append("clickAlert", this.clickAlert.toDocument());
        }
        if (this.location != null && this.location.getWorld() != null) {
            doc.append("location", this.location.serialize());
        }
        return doc;
    }

    public String getId() { return id; }
    public int getEntityId() { return entityId; }
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public Location getLocation() { return location; }
    public Optional<Skin> getSkin() { return Optional.ofNullable(skin); }
    public String getSkinUsername() { return skinUsername; }
    public String getActionType() { return actionType; }
    public List<String> getActionValues() { return actionValues; }
    public String getServer() { return server; }
    public boolean isLookAtPlayer() { return lookAtPlayer; }
    public Optional<ClickAlert> getClickAlert() { return Optional.ofNullable(clickAlert); }
    public Set<UUID> getViewers() { return viewers; }
    public boolean isGlobal() { return isGlobal; }
    public void setLocation(Location location) { this.location = location; }
    public void setSkin(Skin skin, String skinUsername) {
        this.skin = skin;
        this.skinUsername = skinUsername;
        this.skinUrl = null;
    }
    public void setAction(String actionType, List<String> actionValues) {
        this.actionType = actionType;
        this.actionValues = actionValues;
    }
    public void setLookAtPlayer(boolean lookAtPlayer) { this.lookAtPlayer = lookAtPlayer; }
    public void setClickAlert(ClickAlert clickAlert) { this.clickAlert = clickAlert; }
    public void setGlobal(boolean global) { this.isGlobal = global; }
    public void addViewer(UUID viewerId) { this.viewers.add(viewerId); }
    public void removeViewer(UUID viewerId) { this.viewers.remove(viewerId); }
}
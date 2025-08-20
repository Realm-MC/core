package br.com.realmmc.core.npc;

import br.com.realmmc.core.npc.actions.ActionType;
import java.util.List;

public class NPC {

    private final String id;
    private final String displayName;
    private final String skinOwner;
    private final String skinUrl;
    private final String skinTexture;
    private final String skinSignature;
    private final String world;
    private final String server;
    private final List<String> hologramLines;
    private final ActionType actionType;
    private final List<String> actionValue;

    public NPC(String id, String displayName, String skinOwner, String skinUrl, String skinTexture, String skinSignature, String world, String server, List<String> hologramLines, ActionType actionType, List<String> actionValue) {
        this.id = id;
        this.displayName = displayName;
        this.skinOwner = skinOwner;
        this.skinUrl = skinUrl;
        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;
        this.world = world;
        this.server = server;
        this.hologramLines = hologramLines;
        this.actionType = actionType;
        this.actionValue = actionValue;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getSkinOwner() { return skinOwner; }
    public String getSkinUrl() { return skinUrl; }
    public String getSkinTexture() { return skinTexture; }
    public String getSkinSignature() { return skinSignature; }
    public String getWorld() { return world; }
    public String getServer() { return server; }
    public List<String> getHologramLines() { return hologramLines; }
    public ActionType getActionType() { return actionType; }
    public List<String> getActionValue() { return actionValue; }
}
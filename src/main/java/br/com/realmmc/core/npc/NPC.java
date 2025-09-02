package br.com.realmmc.core.npc;

import br.com.realmmc.core.npc.actions.ActionType;
import java.util.List;

public class NPC {

    private final String id;
    private final String displayName;
    private final String world;
    private final String server; // Campo adicionado
    private final String skinUrl;
    private final ActionType actionType;
    private final List<String> actionValue;

    public NPC(String id, String displayName, String world, String server, String skinUrl, ActionType actionType, List<String> actionValue) {
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.server = server;
        this.skinUrl = skinUrl;
        this.actionType = actionType;
        this.actionValue = actionValue;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getWorld() { return world; }
    public String getServer() { return server; }
    public String getSkinUrl() { return skinUrl; }
    public ActionType getActionType() { return actionType; }
    public List<String> getActionValue() { return actionValue; }
}
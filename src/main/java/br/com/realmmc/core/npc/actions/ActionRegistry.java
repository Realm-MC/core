package br.com.realmmc.core.npc.actions;

import br.com.realmmc.core.npc.actions.impl.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ActionRegistry {

    private final Map<String, NPCAction> actionMap = new HashMap<>();

    public ActionRegistry() {
        // Registra todas as ações padrão
        register("PLAYER_COMMAND", new PlayerCommandAction());
        register("CONSOLE_COMMAND", new ConsoleCommandAction());
        register("SEND_MESSAGE", new SendMessageAction());
        register("SEND_SEQUENCED_MESSAGE", new SendSequencedMessageAction());
        register("CONNECT_SERVER", new ConnectServerAction());
    }

    public void register(String name, NPCAction action) {
        actionMap.put(name.toUpperCase(), action);
    }

    public Optional<NPCAction> getAction(String name) {
        return Optional.ofNullable(actionMap.get(name.toUpperCase()));
    }

    public Set<String> getActionNames() {
        return actionMap.keySet();
    }
}
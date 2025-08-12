// PASTA: core/src/main/java/br/com/realmmc/core/managers/SpamManager.java
package br.com.realmmc.core.managers;

import br.com.realmmc.core.utils.SimilarityChecker;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpamManager {

    private final Map<UUID, String> lastMessages = new ConcurrentHashMap<>();
    private final DelayManager delayManager;
    private final TranslationsManager translationsManager;

    public SpamManager(DelayManager delayManager, TranslationsManager translationsManager) {
        this.delayManager = delayManager;
        this.translationsManager = translationsManager;
    }

    public boolean isSpam(Player player, String message, String chatDelayKey) {
        if (player.hasPermission("proxy.moderator")) {
            return false;
        }

        long delay = player.hasPermission("proxy.champion") ? 3L : 5L;

        // --- ALTERAÇÃO APLICADA AQUI ---
        if (delayManager.hasDelay(player, chatDelayKey, "general.delay-private-message")) {
            return true;
        }
        delayManager.setDelay(player, chatDelayKey, delay);

        String lastMessage = lastMessages.get(player.getUniqueId());
        if (lastMessage != null) {
            if (lastMessage.equalsIgnoreCase(message)) {
                translationsManager.sendMessage(player, "chat.duplicate-message");
                return true;
            }
            if (SimilarityChecker.isSimilar(lastMessage, message)) {
                translationsManager.sendMessage(player, "chat.similar-message");
                return true;
            }
        }

        updateLastMessage(player, message);
        return false;
    }

    private void updateLastMessage(Player player, String message) {
        lastMessages.put(player.getUniqueId(), message);
    }
}
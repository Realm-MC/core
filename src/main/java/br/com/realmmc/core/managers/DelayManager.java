// PASTA: core/src/main/java/br/com/realmmc/core/managers/DelayManager.java
package br.com.realmmc.core.managers;

import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gerencia o DELAY (tempo de espera) entre a utilização de comandos.
 */
public class DelayManager {

    private final Map<String, Map<UUID, Long>> delays = new ConcurrentHashMap<>();
    private static final String DEFAULT_MESSAGE_KEY = "general.delay-command-message"; // Mensagem padrão para comandos

    public void setDelay(Player player, String key, long seconds) {
        if (player.hasPermission("proxy.champion")) {
            return;
        }
        long expirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
        delays.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(player.getUniqueId(), expirationTime);
    }

    /**
     * Verifica o delay usando uma chave de mensagem específica.
     * @param player O jogador.
     * @param key A chave da ação.
     * @param messageKey A chave da mensagem no arquivo de tradução.
     * @return true se estiver em delay, false caso contrário.
     */
    public boolean hasDelay(Player player, String key, String messageKey) {
        if (player.hasPermission("proxy.champion")) {
            return false;
        }

        Map<UUID, Long> commandDelays = delays.get(key);
        if (commandDelays == null || !commandDelays.containsKey(player.getUniqueId())) {
            return false;
        }

        long expirationTime = commandDelays.get(player.getUniqueId());

        if (System.currentTimeMillis() < expirationTime) {
            long remainingMillis = expirationTime - System.currentTimeMillis();
            long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) + 1;

            String message = CoreAPI.getInstance().getTranslationsManager().getRawMessage(
                    messageKey, "time", String.valueOf(remainingSeconds)
            );

            CoreAPI.getInstance().getActionBarManager().setMessage(player, ActionBarManager.MessagePriority.HIGH, "delay_" + key, message, 2);
            CoreAPI.getInstance().getSoundManager().playError(player);

            return true;
        }

        commandDelays.remove(player.getUniqueId());
        return false;
    }

    /**
     * Verifica o delay usando a mensagem padrão para comandos.
     */
    public boolean hasDelay(Player player, String key) {
        return hasDelay(player, key, DEFAULT_MESSAGE_KEY);
    }

    public void clearDelays(UUID uuid) {
        for (Map<UUID, Long> commandDelays : delays.values()) {
            commandDelays.remove(uuid);
        }
    }
}
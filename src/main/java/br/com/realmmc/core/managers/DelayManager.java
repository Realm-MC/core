package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gerencia os cooldowns para comandos, prevenindo o uso excessivo.
 * Versão para o plugin Core (Spigot/Paper).
 */
public class DelayManager {

    private final TranslationsManager translations;
    private final SoundManager soundManager;
    private final Map<String, Map<UUID, Long>> cooldowns;

    public DelayManager(Main plugin) {
        this.translations = plugin.getTranslationsManager();
        this.soundManager = plugin.getSoundManager();
        this.cooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Define um cooldown para um jogador em um comando específico.
     * A duração é determinada pelas permissões do jogador.
     *
     * @param player O jogador a receber o cooldown.
     * @param commandKey A chave única do comando (ex: "home", "kit").
     */
    public void setCooldown(Player player, String commandKey) {
        // As permissões são globais via LuckPerms, então podemos usar as mesmas.
        if (player.hasPermission("proxy.moderador")) {
            return;
        }

        long durationInSeconds = player.hasPermission("proxy.champion") ? 3L : 5L;
        long expirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(durationInSeconds);

        cooldowns.computeIfAbsent(commandKey, k -> new ConcurrentHashMap<>()).put(player.getUniqueId(), expirationTime);
    }

    /**
     * Verifica se um jogador está em cooldown para um comando específico.
     * Se estiver, envia uma mensagem de erro e retorna true.
     *
     * @param player O jogador a ser verificado.
     * @param commandKey A chave única do comando.
     * @return true se o jogador estiver em cooldown, false caso contrário.
     */
    public boolean isCoolingDown(Player player, String commandKey) {
        if (player.hasPermission("proxy.moderador")) {
            return false;
        }

        Map<UUID, Long> commandCooldowns = cooldowns.get(commandKey);
        if (commandCooldowns == null || !commandCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }

        long expirationTime = commandCooldowns.get(player.getUniqueId());

        if (System.currentTimeMillis() < expirationTime) {
            long remainingMillis = expirationTime - System.currentTimeMillis();
            long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) + 1;

            translations.sendMessage(player, "general.cooldown-active", "time", String.valueOf(remainingSeconds));
            soundManager.playError(player); // Adaptado para o SoundManager do Core
            return true;
        }

        commandCooldowns.remove(player.getUniqueId());
        return false;
    }

    /**
     * Limpa todos os cooldowns de um jogador ao desconectar.
     * @param uuid O UUID do jogador.
     */
    public void clearCooldowns(UUID uuid) {
        for (Map<UUID, Long> commandCooldowns : cooldowns.values()) {
            commandCooldowns.remove(uuid);
        }
    }
}
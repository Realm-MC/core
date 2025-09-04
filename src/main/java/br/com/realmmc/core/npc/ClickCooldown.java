package br.com.realmmc.core.npc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClickCooldown {
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TIME_MS = 2000; // 2 segundos

    public static boolean hasCooldown(UUID playerId) {
        return cooldowns.containsKey(playerId) && System.currentTimeMillis() < cooldowns.get(playerId);
    }

    public static void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis() + COOLDOWN_TIME_MS);
    }
}
package br.com.realmmc.core.particle;

import org.bukkit.entity.Player;

public interface ParticleEffect {
    String getId();
    void render();
    boolean isExpired();
    Player getTargetPlayer();
}
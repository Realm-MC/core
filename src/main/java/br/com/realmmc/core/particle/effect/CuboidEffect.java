package br.com.realmmc.core.particle.effect;

import br.com.realmmc.core.particle.ParticleEffect;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class CuboidEffect implements ParticleEffect {

    private final String id;
    private final Player targetPlayer;
    private final BoundingBox bounds;
    private final Particle particle;
    private final long expirationTime;

    public CuboidEffect(String id, Player targetPlayer, BoundingBox bounds, Particle particle, int durationTicks) {
        this.id = id;
        this.targetPlayer = targetPlayer;
        this.bounds = bounds;
        this.particle = particle;
        this.expirationTime = (durationTicks == -1) ? -1 : System.currentTimeMillis() + (durationTicks * 50L);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Player getTargetPlayer() {
        return targetPlayer;
    }

    @Override
    public boolean isExpired() {
        if (targetPlayer == null || !targetPlayer.isOnline()) return true;
        if (expirationTime == -1) return false;
        return System.currentTimeMillis() > expirationTime;
    }

    @Override
    public void render() {
        if (isExpired()) return;

        double minX = bounds.getMinX();
        double minY = targetPlayer.getLocation().getY() - 1; // Baseia a altura no jogador
        double minZ = bounds.getMinZ();
        double maxX = bounds.getMaxX();
        double maxY = minY + 4; // Cria uma "parede" de 5 blocos de altura
        double maxZ = bounds.getMaxZ();
        double density = 0.5; // Espaçamento entre partículas

        for (double x = minX; x <= maxX; x += density) {
            targetPlayer.spawnParticle(particle, x, minY, minZ, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, x, maxY, minZ, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, x, minY, maxZ, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, x, maxY, maxZ, 1, 0, 0, 0, 0);
        }
        for (double y = minY; y <= maxY; y += density) {
            targetPlayer.spawnParticle(particle, minX, y, minZ, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, maxX, y, minZ, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, minX, y, maxZ, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, maxX, y, maxZ, 1, 0, 0, 0, 0);
        }
        for (double z = minZ; z <= maxZ; z += density) {
            targetPlayer.spawnParticle(particle, minX, minY, z, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, maxX, minY, z, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, minX, maxY, z, 1, 0, 0, 0, 0);
            targetPlayer.spawnParticle(particle, maxX, maxY, z, 1, 0, 0, 0, 0);
        }
    }
}
package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.particle.ParticleEffect;
import br.com.realmmc.core.particle.effect.CuboidEffect;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ParticleManager {

    private final Main plugin;
    private final List<ParticleEffect> activeEffects = new CopyOnWriteArrayList<>();

    public ParticleManager(Main plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeEffects.isEmpty()) {
                    return;
                }
                for (ParticleEffect effect : activeEffects) {
                    if (effect.isExpired()) {
                        activeEffects.remove(effect);
                        continue;
                    }
                    effect.render();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Roda 10x por segundo para um efeito fluido
    }

    /**
     * Desenha as bordas de um cuboide, visível apenas para um jogador específico.
     * @param player O jogador que verá o efeito.
     * @param bounds O BoundingBox que define o cuboide.
     * @param particle O tipo de partícula a ser usada.
     * @param durationTicks A duração do efeito em ticks (-1 para infinito).
     */
    public void drawCuboidForPlayer(Player player, BoundingBox bounds, Particle particle, int durationTicks) {
        if (player == null || !player.isOnline() || bounds == null || particle == null) return;

        // Gera um ID único para este efeito de borda
        String effectId = "border_" + player.getUniqueId();
        removeEffect(effectId); // Remove qualquer efeito de borda antigo para este jogador

        ParticleEffect effect = new CuboidEffect(effectId, player, bounds, particle, durationTicks);
        activeEffects.add(effect);
    }

    /**
     * Verifica se um efeito com um ID específico está ativo.
     * @param id O ID do efeito.
     * @return true se o efeito estiver ativo, false caso contrário.
     */
    public boolean hasEffect(String id) {
        return activeEffects.stream().anyMatch(effect -> effect.getId().equals(id));
    }

    /**
     * Remove um efeito da lista de renderização pelo seu ID.
     * @param id O ID único do efeito a ser removido.
     */
    public void removeEffect(String id) {
        activeEffects.removeIf(effect -> effect.getId().equals(id));
    }

    /**
     * Remove todos os efeitos associados a um jogador (usado no onQuit).
     * @param player O jogador.
     */
    public void removeAllEffectsForPlayer(Player player) {
        if (player == null) return;
        activeEffects.removeIf(effect -> effect.getTargetPlayer() != null && effect.getTargetPlayer().getUniqueId().equals(player.getUniqueId()));
    }
}
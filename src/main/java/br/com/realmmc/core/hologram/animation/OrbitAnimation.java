package br.com.realmmc.core.hologram.animation;

import br.com.realmmc.core.hologram.Hologram;
import br.com.realmmc.core.hologram.packets.HologramPacketController;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import org.bukkit.entity.Player;

public class OrbitAnimation implements Animation {

    private final double radius;
    private final double speed;
    private long startTime = System.currentTimeMillis();

    public OrbitAnimation(double radius, double speed) {
        this.radius = radius;
        this.speed = speed;
    }

    @Override
    public void tick(Player player, int entityId, Hologram hologram) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        double angle = (elapsedTime / 1000.0) * speed;

        float x = (float) (radius * Math.cos(angle));
        float z = (float) (radius * Math.sin(angle));

        Transformation transformation = new Transformation(
                new Vector3f(x, 0, z), // Translação (posição)
                new AxisAngle4f((float) angle, 0, 1, 0), // Rotação Esquerda/Direita
                new Vector3f(1.5f, 1.5f, 1.5f), // Escala (tamanho)
                new AxisAngle4f()  // Rotação Direita/Esquerda
        );

        HologramPacketController.updateDisplayTransformation(player, entityId, transformation);
    }
}
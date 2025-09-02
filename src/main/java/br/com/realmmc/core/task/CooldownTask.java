package br.com.realmmc.core.task;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.ActionBarManager;
import br.com.realmmc.core.managers.CooldownManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CooldownTask extends BukkitRunnable {

    private final Player player;
    private int secondsRemaining;
    private final Runnable onFinish;
    private final Runnable onCancel;
    private final CooldownManager manager;
    private final Location startLocation;
    private boolean wasCancelledByAction = false;

    public CooldownTask(Player player, int seconds, Runnable onFinish, Runnable onCancel, CooldownManager manager) {
        this.player = player;
        this.secondsRemaining = seconds;
        this.onFinish = onFinish;
        this.onCancel = onCancel;
        this.manager = manager;
        this.startLocation = player.getLocation();
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            this.cancel();
            return;
        }

        // A verificação de movimento agora está no CooldownManager, que é um Listener

        if (secondsRemaining <= 0) {
            onFinish.run();
            this.cancel();
            return;
        }

        // CORREÇÃO: Acessando o enum MessagePriority corretamente
        CoreAPI.getInstance().getActionBarManager().setMessage(player,
                ActionBarManager.MessagePriority.HIGH,
                "teleport_cooldown",
                CoreAPI.getInstance().getTranslationsManager().getMessage("general.cooldown-active-teleport", "time", String.valueOf(secondsRemaining)),
                2
        );
        CoreAPI.getInstance().getSoundManager().playClick(player);
        secondsRemaining--;
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        // CORREÇÃO: Usando o método que agora existe no manager
        manager.removeCooldown(player.getUniqueId());
    }

    /**
     * Chamado quando o cooldown é cancelado por uma ação (mover, entrar em combate, etc.)
     */
    public void customCancel() {
        if (isCancelled()) return;

        this.wasCancelledByAction = true;
        onCancel.run(); // Executa a ação de cancelamento (ex: mensagem de combate)
        this.cancel();
    }

    private boolean hasMoved() {
        Location currentLocation = player.getLocation();
        return startLocation.getWorld() != currentLocation.getWorld() ||
                startLocation.getBlockX() != currentLocation.getBlockX() ||
                startLocation.getBlockY() != currentLocation.getBlockY() ||
                startLocation.getBlockZ() != currentLocation.getBlockZ();
    }
}
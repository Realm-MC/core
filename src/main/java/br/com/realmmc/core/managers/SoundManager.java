package br.com.realmmc.core.managers;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Gerencia a reprodução de sons padronizados para jogadores.
 * Esta classe é utilitária e não precisa ser instanciada por servidor,
 * por isso os métodos podem ser estáticos ou a classe ser instanciada uma vez.
 * Para consistência, mantemos o padrão de gestor.
 */
public class SoundManager {

    // Constantes de som para fácil referência e manutenção.
    private static final SoundInfo SUCCESS_SOUND = new SoundInfo(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
    private static final SoundInfo ERROR_SOUND = new SoundInfo(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
    private static final SoundInfo CLICK_SOUND = new SoundInfo(Sound.UI_BUTTON_CLICK, 0.7f, 1.8f);
    private static final SoundInfo TELEPORT_SOUND = new SoundInfo(Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    private static final SoundInfo LEVEL_UP_SOUND = new SoundInfo(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    private static final SoundInfo NOTIFICATION_SOUND = new SoundInfo(Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);

    /**
     * Reproduz um som de sucesso para o jogador.
     * @param player O jogador para quem o som será tocado.
     */
    public void playSuccess(Player player) {
        playSound(player, SUCCESS_SOUND);
    }

    /**
     * Reproduz um som de erro para o jogador.
     * @param player O jogador para quem o som será tocado.
     */
    public void playError(Player player) {
        playSound(player, ERROR_SOUND);
    }

    /**
     * Reproduz um som de clique para o jogador.
     * @param player O jogador para quem o som será tocado.
     */
    public void playClick(Player player) {
        playSound(player, CLICK_SOUND);
    }

    /**
     * Reproduz um som de teleporte para o jogador.
     * @param player O jogador para quem o som será tocado.
     */
    public void playTeleport(Player player) {
        playSound(player, TELEPORT_SOUND);
    }

    /**
     * Reproduz um som de "level up" ou conquista.
     * @param player O jogador para quem o som será tocado.
     */
    public void playLevelUp(Player player) {
        playSound(player, LEVEL_UP_SOUND);
    }

    /**
     * Reproduz um som de notificação genérica.
     * @param player O jogador para quem o som será tocado.
     */
    public void playNotification(Player player) {
        playSound(player, NOTIFICATION_SOUND);
    }

    /**
     * Método base para tocar um som para um jogador em sua localização atual.
     * @param player O jogador.
     * @param soundInfo As informações do som a ser tocado.
     */
    private void playSound(Player player, SoundInfo soundInfo) {
        if (player == null || !player.isOnline() || soundInfo == null) {
            return;
        }
        player.playSound(player.getLocation(), soundInfo.sound, soundInfo.volume, soundInfo.pitch);
    }

    /**
     * Método base para tocar um som em uma localização específica.
     * @param location A localização onde o som será tocado.
     * @param soundInfo As informações do som a ser tocado.
     */
    private void playSoundAtLocation(Location location, SoundInfo soundInfo) {
        if (location == null || location.getWorld() == null || soundInfo == null) {
            return;
        }
        location.getWorld().playSound(location, soundInfo.sound, soundInfo.volume, soundInfo.pitch);
    }

    /**
     * Classe interna para armazenar informações de um som (tipo, volume, tom).
     */
    private record SoundInfo(Sound sound, float volume, float pitch) {}
}
package br.com.realmmc.core.npc.skin;

/**
 * Representa os dados de uma skin do Minecraft, contendo a textura e a assinatura.
 * Esta classe é imutável para garantir a integridade dos dados da skin.
 */
public record Skin(String value, String signature) {
}
// PASTA: core/src/main/java/br/com/realmmc/core/utils/InteractiveChatFormatter.java
package br.com.realmmc.core.utils;

import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Utilitário para formatar e enviar mensagens de chat interativas
 * com base em uma estrutura de ConfigurationSection.
 */
public final class InteractiveChatFormatter {

    /**
     * Formata e envia uma mensagem de chat para todos os jogadores.
     * @param player O jogador que enviou a mensagem.
     * @param message O conteúdo da mensagem (já colorido).
     * @param formatSection A seção de configuração com a estrutura do formato.
     * @param formattedName O nome já formatado do jogador (ex: [Admin] Nick), vindo do RealmPlayer.
     */
    public static void formatAndSend(Player player, String message, ConfigurationSection formatSection, String formattedName) {
        if (formatSection == null) {
            // Fallback para um formato simples se a seção de configuração não for encontrada.
            player.getServer().broadcastMessage(ColorAPI.format(formattedName + "&f: &r" + message));
            CoreAPI.getInstance().getPlugin().getLogger().warning("A chave de formato de chat não foi encontrada ou é inválida. Usando formato de chat de fallback.");
            return;
        }

        // A chamada assíncrona foi removida daqui, tornando o método mais simples e direto.

        JsonMessage jsonMessage = null;

        for (String partKey : formatSection.getKeys(false)) {
            ConfigurationSection part = formatSection.getConfigurationSection(partKey);
            if (part == null) continue;

            // Usa o 'formattedName' recebido como parâmetro.
            String text = part.getString("text", "")
                    .replace("{player_name}", formattedName)
                    .replace("{player_plain_name}", player.getName())
                    .replace("{message}", message);

            if (jsonMessage == null) {
                jsonMessage = JsonMessage.create(text);
            } else {
                jsonMessage.then(text);
            }

            String hover = part.getString("hover", "");
            if (!hover.isEmpty()) {
                jsonMessage.withHover(hover.replace("{player_name}", formattedName).replace("{player_plain_name}", player.getName()));
            }

            String suggest = part.getString("suggest", "");
            if (!suggest.isEmpty()) {
                jsonMessage.suggest(suggest.replace("{player_plain_name}", player.getName()));
            }
        }

        if (jsonMessage != null) {
            jsonMessage.sendToAll();
        }
    }
}
// PASTA: core/src/main/java/br/com/realmmc/core/utils/ColorAPI.java
package br.com.realmmc.core.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe utilitária para formatação de cores em strings.
 * Suporta códigos de cor legados (&) e códigos de cor HEX (&#RRGGBB).
 *
 * @author Gemini & xxxlc (Versão 2.0)
 */
public final class ColorAPI {

    // Padrão para encontrar códigos de cor no formato &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorAPI() {
        // Construtor privado para impedir a instanciação
    }

    /**
     * Formata uma string, traduzindo códigos de cor legados (&) e HEX (&#RRGGBB)
     * para uma String colorida com '§', pronta para ser usada em qualquer lugar do Spigot/Paper.
     * @param text O texto a ser formatado.
     * @return O texto formatado com cores.
     */
    public static String format(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Primeiro, traduz os códigos HEX
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder(text.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(builder, ChatColor.of("#" + group).toString());
        }
        matcher.appendTail(builder);

        // Depois, traduz os códigos de cor legados (ex: &c)
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    /**
     * Formata uma string, traduzindo todos os códigos de cor diretamente
     * para o array de BaseComponent do BungeeCord, usado em JsonMessages.
     * @param text O texto a ser formatado.
     * @return Um array de BaseComponent pronto para ser enviado ao jogador.
     */
    public static BaseComponent[] formatBungee(String text) {
        if (text == null || text.isEmpty()) {
            return new BaseComponent[0];
        }
        // O método format() já faz todo o trabalho pesado de traduzir HEX e &,
        // então só precisamos converter o resultado final para o formato do Bungee.
        String translatedText = format(text);
        return TextComponent.fromLegacyText(translatedText);
    }
}
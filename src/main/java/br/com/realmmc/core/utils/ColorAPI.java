package br.com.realmmc.core.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe utilitária para formatação de cores em strings.
 * Suporta códigos de cor legados (&) e códigos de cor HEX (&#RRGGBB).
 */
public final class ColorAPI {

    // Padrão para encontrar códigos de cor HEX no formato &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorAPI() {
        // Construtor privado para impedir a instanciação de classes utilitárias
    }

    /**
     * Formata uma string, traduzindo códigos de cor legados e HEX.
     * @param text O texto a ser formatado.
     * @return O texto formatado com cores, pronto para ser enviado ao jogador.
     */
    public static String format(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Traduz códigos HEX para o formato do BungeeCord/Paper
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);

        // Traduz códigos de cor legados (ex: &a, &l)
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
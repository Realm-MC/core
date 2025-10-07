package br.com.realmmc.core.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorAPI {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    public static String format(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, ChatColor.of(matcher.group()).toString());
        }
        matcher.appendTail(builder);
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    public static BaseComponent[] formatBungee(String text) {
        return TextComponent.fromLegacyText(format(text));
    }

    /**
     * NOVO MÉTODO: Remove todos os códigos de cor (& e §) de uma string.
     * @param text O texto a ser limpo.
     * @return O texto sem formatação de cor.
     */
    public static String stripColors(String text) {
        if (text == null) {
            return null;
        }
        return ChatColor.stripColor(format(text));
    }
}
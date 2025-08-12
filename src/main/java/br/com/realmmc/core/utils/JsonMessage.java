package br.com.realmmc.core.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class JsonMessage {

    private final ComponentBuilder builder;

    private JsonMessage(BaseComponent[] initialComponents) {
        this.builder = new ComponentBuilder();
        for (BaseComponent component : initialComponents) {
            builder.append(component, ComponentBuilder.FormatRetention.NONE);
        }
    }

    public static JsonMessage create(String initialText) {
        return new JsonMessage(ColorAPI.formatBungee(initialText));
    }

    public JsonMessage then(String text) {
        builder.append(ColorAPI.formatBungee(text), ComponentBuilder.FormatRetention.NONE);
        return this;
    }

    public JsonMessage withHover(String... lines) {
        ComponentBuilder hoverBuilder = new ComponentBuilder();
        for (int i = 0; i < lines.length; i++) {
            hoverBuilder.append(ColorAPI.formatBungee(lines[i]), ComponentBuilder.FormatRetention.NONE);
            if (i < lines.length - 1) hoverBuilder.append("\n");
        }
        builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));
        return this;
    }

    public JsonMessage withClick(ClickEvent.Action action, String value) {
        builder.event(new ClickEvent(action, value));
        return this;
    }

    public JsonMessage suggest(String command) { return withClick(ClickEvent.Action.SUGGEST_COMMAND, command); }
    public JsonMessage command(String command) { return withClick(ClickEvent.Action.RUN_COMMAND, command); }
    public JsonMessage url(String url) { return withClick(ClickEvent.Action.OPEN_URL, url); }
    public JsonMessage copy(String text) { return withClick(ClickEvent.Action.COPY_TO_CLIPBOARD, text); }

    public void send(CommandSender sender) {
        BaseComponent[] finalMessage = builder.create();
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(finalMessage);
        } else {
            sender.sendMessage(BaseComponent.toLegacyText(finalMessage));
        }
    }

    public void sendToAll() {
        BaseComponent[] components = builder.create();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(components);
        }
        Bukkit.getConsoleSender().sendMessage(BaseComponent.toLegacyText(components));
    }
}
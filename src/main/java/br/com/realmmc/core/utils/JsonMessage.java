package br.com.realmmc.core.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Classe utilitária para criar mensagens de texto interativas (JSON) de forma fluente.
 * Suporta eventos de clique e hover, usando a API de chat do BungeeCord, que é
 * nativamente suportada pelo Spigot/Paper.
 *
 * <p><b>Exemplo de Uso:</b></p>
 * <pre>
 * {@code
 * JsonMessage.create("&aClique ")
 * .then("&a&lAQUI")
 * .command("/say olá")
 * .withHover("&eExecuta o comando /say olá")
 * .then(" &apara dizer olá!")
 * .send(player);
 * }
 * </pre>
 */
public final class JsonMessage {

    private final ComponentBuilder builder;
    private BaseComponent[] currentPart;

    private JsonMessage(String initialText) {
        // Inicializa o builder com a primeira parte do texto
        this.builder = new ComponentBuilder(ColorAPI.format(initialText));
        // A "parte atual" é o último segmento adicionado, sobre o qual as ações (hover, click) serão aplicadas
        this.currentPart = new ComponentBuilder(ColorAPI.format(initialText)).create();
    }

    /**
     * Ponto de entrada para criar uma nova instância de JsonMessage.
     * @param initialText O texto inicial da mensagem.
     * @return Uma nova instância do builder JsonMessage.
     */
    public static JsonMessage create(String initialText) {
        return new JsonMessage(initialText);
    }

    /**
     * Adiciona um novo segmento de texto à mensagem. Eventos de clique/hover
     * aplicados em seguida afetarão este novo segmento.
     * @param text O texto a ser adicionado.
     * @return A instância atual do builder para encadeamento de métodos.
     */
    public JsonMessage then(String text) {
        // Salva a parte anterior no builder principal antes de criar uma nova
        saveCurrentPart();
        this.currentPart = new ComponentBuilder(ColorAPI.format(text)).create();
        return this;
    }

    /**
     * Adiciona um texto de hover (tooltip) ao último segmento de texto adicionado.
     * @param lines As linhas de texto que aparecerão no hover. Cada string é uma nova linha.
     * @return A instância atual do builder.
     */
    public JsonMessage withHover(String... lines) {
        if (currentPart != null) {
            ComponentBuilder hoverBuilder = new ComponentBuilder();
            for (int i = 0; i < lines.length; i++) {
                hoverBuilder.append(ColorAPI.format(lines[i]), ComponentBuilder.FormatRetention.NONE);
                if (i < lines.length - 1) hoverBuilder.append("\n");
            }
            for (BaseComponent component : currentPart) {
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));
            }
        }
        return this;
    }

    /**
     * Adiciona um evento de clique ao último segmento de texto adicionado.
     * @param action A ação a ser executada no clique (ex: RUN_COMMAND).
     * @param value O valor da ação (ex: um comando ou URL).
     * @return A instância atual do builder.
     */
    public JsonMessage withClick(ClickEvent.Action action, String value) {
        if (currentPart != null) {
            for (BaseComponent component : currentPart) {
                component.setClickEvent(new ClickEvent(action, value));
            }
        }
        return this;
    }

    // --- Métodos de Conveniência para Ações de Clique Comuns ---

    public JsonMessage suggest(String command) { return withClick(ClickEvent.Action.SUGGEST_COMMAND, command); }
    public JsonMessage command(String command) { return withClick(ClickEvent.Action.RUN_COMMAND, command); }
    public JsonMessage url(String url) { return withClick(ClickEvent.Action.OPEN_URL, url); }
    public JsonMessage copy(String text) { return withClick(ClickEvent.Action.COPY_TO_CLIPBOARD, text); }

    /**
     * Compila a mensagem JSON e a envia para um CommandSender.
     * @param sender O destinatário da mensagem (Player ou Console).
     */
    public void send(CommandSender sender) {
        saveCurrentPart();
        BaseComponent[] finalMessage = builder.create();

        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(finalMessage);
        } else {
            // Converte para texto legado para ser legível no console
            sender.sendMessage(BaseComponent.toLegacyText(finalMessage));
        }
    }

    /**
     * Método interno para anexar a `currentPart` (último segmento trabalhado) ao builder principal.
     */
    private void saveCurrentPart() {
        if (currentPart != null) {
            builder.append(currentPart, ComponentBuilder.FormatRetention.NONE);
            currentPart = null;
        }
    }
}
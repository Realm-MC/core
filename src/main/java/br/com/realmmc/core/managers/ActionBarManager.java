package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.utils.ColorAPI;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActionBarManager {

    private final Main plugin;
    private final Map<UUID, Map<MessagePriority, List<Message>>> activeMessages;
    private final String separator;

    public enum MessagePriority {
        HIGH, MEDIUM, LOW
    }

    public ActionBarManager(Main plugin) {
        this.plugin = plugin;
        this.activeMessages = new ConcurrentHashMap<>();
        this.separator = plugin.getTranslationsManager().getRawMessage("actionbar.separator");
        startUpdateTask();
    }

    public void setMessage(Player player, MessagePriority priority, String id, String text, int durationSeconds) {
        if (player == null || !player.isOnline()) return;

        long expiration = (durationSeconds == -1) ? -1 : System.currentTimeMillis() + (durationSeconds * 1000L);
        Message newMessage = new Message(id, text, expiration);

        Map<MessagePriority, List<Message>> playerMessages = activeMessages.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(MessagePriority.class));
        List<Message> messageList = playerMessages.computeIfAbsent(priority, k -> new ArrayList<>());

        messageList.removeIf(msg -> msg.getId().equals(id));
        messageList.add(newMessage);
    }

    public void clearMessage(Player player, String id) {
        if (player == null) return;

        Map<MessagePriority, List<Message>> playerMessages = activeMessages.get(player.getUniqueId());
        if (playerMessages != null) {
            playerMessages.values().forEach(list -> list.removeIf(msg -> msg.getId().equals(id)));
        }
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new ArrayList<>(activeMessages.keySet())) {
                    Player player = plugin.getServer().getPlayer(uuid);

                    if (player == null || !player.isOnline()) {
                        activeMessages.remove(uuid);
                        continue;
                    }

                    Map<MessagePriority, List<Message>> playerMessages = activeMessages.get(uuid);
                    if (playerMessages == null) continue;

                    playerMessages.values().forEach(list -> list.removeIf(Message::isExpired));
                    playerMessages.entrySet().removeIf(entry -> entry.getValue().isEmpty());

                    if (playerMessages.isEmpty()) {
                        activeMessages.remove(uuid);
                        sendRawActionBar(player, " ");
                        continue;
                    }

                    sendRawActionBar(player, composeMessage(playerMessages));
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 10L);
    }

    private String composeMessage(Map<MessagePriority, List<Message>> messages) {
        return messages.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().stream())
                .map(Message::getText)
                .collect(Collectors.joining(this.separator));
    }

    private void sendRawActionBar(Player player, String message) {
        if (player != null && player.isOnline()) {
            player.sendActionBar(ColorAPI.format(message));
        }
    }

    private static class Message {
        private final String id;
        private final String text;
        private final long expiration;

        public Message(String id, String text, long expiration) {
            this.id = id;
            this.text = text;
            this.expiration = expiration;
        }

        public String getId() { return id; }
        public String getText() { return text; }
        public boolean isExpired() {
            return expiration != -1 && System.currentTimeMillis() > expiration;
        }
    }
}
package br.com.realmmc.core.commands;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.NPCManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NpcCommand implements CommandExecutor, TabCompleter {

    private final NPCManager npcManager;
    private final TranslationsManager translations;
    private static final String PERMISSION = "core.admin.npc";

    public NpcCommand() {
        this.npcManager = CoreAPI.getInstance().getNpcManager();
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            translations.sendMessage(sender, "commands.npc.player-only");
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            translations.sendNoPermissionMessage(player, "Gerente");
            return true;
        }

        if (args.length < 1) {
            translations.sendMessage(player, "commands.npc.usage");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "criar": handleCreate(player, args); break;
            case "deletar": handleDelete(player, args); break;
            case "list": handleList(player); break;
            case "info": handleInfo(player, args); break;
            default:
                translations.sendMessage(player, "commands.npc.invalid-subcommand");
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.create.usage");
            return;
        }
        String id = args[1];

        if (npcManager.getDefinition(id) == null) {
            translations.sendMessage(player, "commands.npc.create.not-found-in-config", "id", id);
            return;
        }

        if (npcManager.isSpawned(id)) {
            translations.sendMessage(player, "commands.npc.create.already-spawned", "id", id);
            return;
        }

        npcManager.setLocationAndSpawn(id, player.getLocation());
        translations.sendMessage(player, "commands.npc.create.success", "id", id);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.delete.usage");
            return;
        }
        String id = args[1];

        if (!npcManager.isSpawned(id)) {
            translations.sendMessage(player, "commands.npc.delete.not-spawned", "id", id);
            return;
        }

        npcManager.deleteAndDespawn(id);
        translations.sendMessage(player, "commands.npc.delete.success", "id", id);
    }

    private void handleList(Player player) {
        translations.sendMessage(player, "commands.npc.list.header");
        Set<String> definedIds = npcManager.getDefinedIds();

        if (definedIds.isEmpty()) {
            translations.sendMessage(player, "commands.npc.list.empty");
            return;
        }

        for (String id : definedIds) {
            boolean isSpawned = npcManager.isSpawned(id);
            String status = isSpawned
                    ? translations.getRawMessage("commands.npc.list.status-spawned")
                    : translations.getRawMessage("commands.npc.list.status-not-spawned");
            translations.sendMessage(player, "commands.npc.list.line-format", "id", id, "status", status);
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.info.usage");
            return;
        }
        String id = args[1];
        NPC definition = npcManager.getDefinition(id);

        if (definition == null) {
            translations.sendMessage(player, "commands.npc.info.not-found", "id", id);
            return;
        }

        translations.sendMessage(player, "commands.npc.info.header", "id", id);
        translations.sendMessage(player, "commands.npc.info.line-name", "display_name", definition.getDisplayName());
        translations.sendMessage(player, "commands.npc.info.line-server", "server", definition.getServer());
        translations.sendMessage(player, "commands.npc.info.line-world", "world", definition.getWorld());
        translations.sendMessage(player, "commands.npc.info.line-action", "action_type", definition.getActionType().toString(), "action_value_size", String.valueOf(definition.getActionValue().size()));

        net.citizensnpcs.api.npc.NPC npc = npcManager.getSpawnedNpc(id.toLowerCase());
        if (npc != null && npc.isSpawned()) {
            Location loc = npc.getStoredLocation();
            String locationString = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
            translations.sendMessage(player, "commands.npc.info.line-location", "location", locationString);
        } else {
            translations.sendMessage(player, "commands.npc.info.line-location-not-spawned");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }

        final String prefix = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            return Stream.of("criar", "deletar", "list", "info")
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            Set<String> definedIds = npcManager.getDefinedIds();

            switch (subCommand) {
                case "criar":
                    return definedIds.stream()
                            .filter(id -> !npcManager.isSpawned(id))
                            .filter(id -> id.toLowerCase().startsWith(prefix))
                            .collect(Collectors.toList());
                case "deletar":
                case "info":
                    return definedIds.stream()
                            .filter(npcManager::isSpawned)
                            .filter(id -> id.toLowerCase().startsWith(prefix))
                            .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}
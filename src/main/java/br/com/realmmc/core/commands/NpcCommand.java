package br.com.realmmc.core.commands;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.NPCManager;
import br.com.realmmc.core.npc.actions.ActionRegistry;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Comando administrativo para gerenciar NPCs baseados em pacotes.
 */
public class NpcCommand implements CommandExecutor, TabCompleter {

    private final String PERMISSION = "core.admin.npc";
    private final NPCManager npcManager;
    private final TranslationsManager translations;

    public NpcCommand() {
        this.npcManager = CoreAPI.getInstance().getNpcManager();
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(sender, "Gerente");
            return true;
        }

        if (!(sender instanceof Player player)) {
            translations.sendMessage(sender, "commands.npc.player-only");
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
            case "tp": handleTp(player, args); break;
            case "tphere": handleTpHere(player, args); break;
            case "setskin": handleSetSkin(player, args); break;
            case "setaction": handleSetAction(player, args); break;
            case "info": handleInfo(player, args); break;
            case "togglelook": handleToggleLook(player, args); break;
            default: translations.sendMessage(player, "commands.npc.usage"); break;
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 4) {
            translations.sendMessage(player, "commands.npc.create.usage");
            return;
        }
        String id = args[1];
        if (npcManager.getNpc(id).isPresent()) {
            translations.sendMessage(player, "commands.npc.create.already-exists", "id", id);
            return;
        }
        String skin = args[2];
        String displayName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        npcManager.create(id, displayName, displayName, player.getLocation(), skin, "NONE", new ArrayList<>());
        translations.sendMessage(player, "commands.npc.create.success", "id", id, "skin", skin);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.delete.usage");
            return;
        }
        String id = args[1];
        if (npcManager.getNpc(id).isEmpty()) {
            translations.sendMessage(player, "commands.npc.error.not-found", "id", id);
            return;
        }
        npcManager.delete(id);
        translations.sendMessage(player, "commands.npc.delete.success", "id", id);
    }

    private void handleList(Player player) {
        translations.sendMessage(player, "commands.npc.list.header");
        if (npcManager.getAllNpcs().isEmpty()) {
            translations.sendMessage(player, "commands.npc.list.empty");
            return;
        }
        npcManager.getAllNpcs().forEach(npc ->
                translations.sendMessage(player, "commands.npc.list.line-format", "id", npc.getId(), "display_name", npc.getDisplayName())
        );
    }

    private void handleTp(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.tp.usage");
            return;
        }
        npcManager.getNpc(args[1]).ifPresentOrElse(npc -> {
            player.teleport(npc.getLocation());
            translations.sendMessage(player, "commands.npc.tp.success", "id", npc.getId());
        }, () -> translations.sendMessage(player, "commands.npc.error.not-found", "id", args[1]));
    }

    private void handleTpHere(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.tphere.usage");
            return;
        }
        npcManager.getNpc(args[1]).ifPresentOrElse(npc -> {
            npc.setLocation(player.getLocation());
            npcManager.updateAndReshow(npc);
            translations.sendMessage(player, "commands.npc.tphere.success", "id", npc.getId());
        }, () -> translations.sendMessage(player, "commands.npc.error.not-found", "id", args[1]));
    }

    private void handleSetSkin(Player player, String[] args) {
        if (args.length != 3) {
            translations.sendMessage(player, "commands.npc.setskin.usage");
            return;
        }
        String id = args[1];
        String skinOwner = args[2];
        npcManager.getNpc(id).ifPresentOrElse(npc -> {
            npcManager.getSkinManager().getSkin(skinOwner).thenAccept(skinOpt -> {
                if (skinOpt.isPresent()) {
                    npc.setSkin(skinOpt.get(), skinOwner);
                    npcManager.updateAndReshow(npc);
                    translations.sendMessage(player, "commands.npc.setskin.success", "id", id, "player", skinOwner);
                } else {
                    translations.sendMessage(player, "commands.npc.setskin.fail", "player", skinOwner);
                }
            });
        }, () -> translations.sendMessage(player, "commands.npc.error.not-found", "id", id));
    }

    private void handleSetAction(Player player, String[] args) {
        if (args.length < 3) {
            translations.sendMessage(player, "commands.npc.setaction.usage");
            return;
        }
        String id = args[1];
        String type = args[2].toUpperCase();

        npcManager.getNpc(id).ifPresentOrElse(npc -> {
            if (type.equalsIgnoreCase("NONE")) {
                npc.setAction("NONE", new ArrayList<>());
                translations.sendMessage(player, "commands.npc.setaction.cleared", "id", id);
                return;
            }
            if (new ActionRegistry().getAction(type).isEmpty()) {
                String types = String.join(", ", new ActionRegistry().getActionNames());
                translations.sendMessage(player, "commands.npc.setaction.invalid-type", "types", types);
                return;
            }
            List<String> values = args.length > 3 ? new ArrayList<>(Arrays.asList(args).subList(3, args.length)) : new ArrayList<>();
            npc.setAction(type, values);
            translations.sendMessage(player, "commands.npc.setaction.success", "id", id, "type", type);
        }, () -> translations.sendMessage(player, "commands.npc.error.not-found", "id", id));
    }

    private void handleToggleLook(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.togglelook.usage");
            return;
        }
        npcManager.getNpc(args[1]).ifPresentOrElse(npc -> {
            boolean newState = !npc.isLookAtPlayer();
            npc.setLookAtPlayer(newState);
            String messageKey = newState ? "commands.npc.togglelook.enabled" : "commands.npc.togglelook.disabled";
            translations.sendMessage(player, messageKey, "id", npc.getId());
        }, () -> translations.sendMessage(player, "commands.npc.error.not-found", "id", args[1]));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.info.usage");
            return;
        }
        npcManager.getNpc(args[1]).ifPresentOrElse(npc -> {
            player.sendMessage(" ");
            translations.sendMessage(player, "commands.npc.info.header", "id", npc.getId());
            translations.sendMessage(player, "commands.npc.info.line-name", "display_name", npc.getDisplayName());
            translations.sendMessage(player, "commands.npc.info.line-skin", "skin_owner", npc.getSkinUsername());

            Location loc = npc.getLocation();
            String locationString = String.format("Mundo: %s, X: %.1f, Y: %.1f, Z: %.1f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
            translations.sendMessage(player, "commands.npc.info.line-location", "location", locationString);

            if (npc.getActionType() == null || npc.getActionType().equalsIgnoreCase("NONE")) {
                translations.sendMessage(player, "commands.npc.info.line-no-action");
            } else {
                translations.sendMessage(player, "commands.npc.info.line-action-type", "action_type", npc.getActionType());
                if (!npc.getActionValues().isEmpty()) {
                    translations.sendMessage(player, "commands.npc.info.line-action-values");
                    npc.getActionValues().forEach(value -> translations.sendMessage(player, "commands.npc.info.line-action-value-entry", "value", value));
                }
            }
            player.sendMessage(" ");
        }, () -> translations.sendMessage(player, "commands.npc.error.not-found", "id", args[1]));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) return List.of();
        String prefix = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            return Stream.of("criar", "deletar", "list", "tphere", "setskin", "setaction", "info", "tp", "togglelook")
                    .filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Stream.of("deletar", "tphere", "setskin", "setaction", "info", "tp", "togglelook").anyMatch(sub::equals)) {
                return npcManager.getNpcIds().stream()
                        .filter(id -> id.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setaction")) {
            return new ActionRegistry().getActionNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
        }

        return List.of();
    }
}
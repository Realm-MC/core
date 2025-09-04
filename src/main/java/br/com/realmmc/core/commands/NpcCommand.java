package br.com.realmmc.core.commands;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.NPCManager;
import br.com.realmmc.core.utils.ColorAPI;
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
        if (!sender.hasPermission(PERMISSION)) {
            translations.sendNoPermissionMessage(sender, "Gerente");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (!(sender instanceof Player player)) {
            if (subCommand.equals("sync")) {
                handleSync(sender);
            } else {
                translations.sendMessage(sender, "commands.npc.player-only");
            }
            return true;
        }

        switch (subCommand) {
            case "sync": handleSync(player); break;
            case "criar": handleCreate(player, args); break;
            case "deletar": handleDelete(player, args); break;
            case "list": handleList(player); break;
            case "tphere": handleTpHere(player, args); break;
            case "setskin": handleSetSkin(player, args); break;
            case "setaction": handleSetAction(player, args); break;
            case "info": handleInfo(player, args); break;
            case "setalert": handleSetAlert(player, args); break;
            default: sendUsage(player); break;
        }
        return true;
    }

    private void handleSync(CommandSender sender) {
        sender.sendMessage(ColorAPI.format("&eSincronizando NPCs do arquivo npcs.yml para o MongoDB..."));
        npcManager.syncFromFile().thenAccept(count -> {
            sender.sendMessage(ColorAPI.format("&aSincronização concluída! " + count + " NPCs foram processados."));
        });
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            translations.sendMessage(player, "commands.npc.create.usage");
            return;
        }
        String id = args[1];
        if (npcManager.getNpc(id).isPresent()) {
            translations.sendMessage(player, "commands.npc.create.already-exists", "id", id);
            return;
        }
        String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        npcManager.createNpc(id, displayName, player.getLocation()).thenRun(() -> {
            translations.sendMessage(player, "commands.npc.create.success", "id", id);
        });
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.delete.usage");
            return;
        }
        String id = args[1];
        if (npcManager.getNpc(id).isEmpty()) {
            translations.sendMessage(player, "commands.npc.delete.not-found", "id", id);
            return;
        }
        npcManager.deleteNpc(id).thenRun(() -> {
            translations.sendMessage(player, "commands.npc.delete.success", "id", id);
        });
    }

    private void handleList(Player player) {
        translations.sendMessage(player, "commands.npc.list.header");
        if (npcManager.getAllNpcs().isEmpty()) {
            translations.sendMessage(player, "commands.npc.list.empty");
            return;
        }
        npcManager.getAllNpcs().forEach(npc -> {
            translations.sendMessage(player, "commands.npc.list.line-format", "id", npc.getId(), "display_name", npc.getDisplayName());
        });
    }

    private void handleTpHere(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.tphere.usage");
            return;
        }
        String id = args[1];
        npcManager.getNpc(id).ifPresentOrElse(npc -> {
            npc.setLocation(player.getLocation());
            npcManager.saveNpc(npc).thenRun(() -> {
                npcManager.spawnOrUpdateNPC(npc);
                translations.sendMessage(player, "commands.npc.tphere.success", "id", id);
            });
        }, () -> translations.sendMessage(player, "commands.npc.delete.not-found", "id", id));
    }

    private void handleSetSkin(Player player, String[] args) {
        if (args.length != 3) {
            translations.sendMessage(player, "commands.npc.setskin.usage");
            return;
        }
        String id = args[1];
        String skinOwner = args[2];
        npcManager.updateSkinFromPlayer(id, skinOwner).thenAccept(success -> {
            if (success) {
                translations.sendMessage(player, "commands.npc.setskin.success", "id", id, "player", skinOwner);
            } else {
                translations.sendMessage(player, "commands.npc.setskin.fail", "player", skinOwner);
            }
        });
    }

    private void handleSetAction(Player player, String[] args) {
        String types = String.join(", ", npcManager.getActionRegistry().getActionNames());
        if (args.length < 3) {
            translations.sendMessage(player, "commands.npc.setaction.usage");
            player.sendMessage(translations.getMessage("commands.npc.setaction.invalid-type", "types", types));
            return;
        }
        String id = args[1];
        String type = args[2].toUpperCase();

        npcManager.getNpc(id).ifPresentOrElse(npc -> {
            if (npcManager.getActionRegistry().getAction(type).isEmpty()) {
                player.sendMessage(translations.getMessage("commands.npc.setaction.invalid-type", "types", types));
                return;
            }
            List<String> values = args.length > 3 ?
                    new ArrayList<>(Arrays.asList(args).subList(3, args.length)) :
                    new ArrayList<>();

            npc.setAction(type, values);
            npcManager.saveNpc(npc).thenRun(() -> {
                translations.sendMessage(player, "commands.npc.setaction.success", "id", id, "type", type);
            });

        }, () -> translations.sendMessage(player, "commands.npc.delete.not-found", "id", id));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.npc.info.usage");
            return;
        }
        String id = args[1];
        npcManager.getNpc(id).ifPresentOrElse(npc -> {
            player.sendMessage(" ");
            translations.sendMessage(player, "commands.npc.info.header", "id", npc.getId());
            translations.sendMessage(player, "commands.npc.info.line-name", "display_name", npc.getDisplayName());

            Location loc = npc.getLocation();
            String locationString = String.format("Mundo: %s, X: %.1f, Y: %.1f, Z: %.1f",
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
            translations.sendMessage(player, "commands.npc.info.line-location", "location", locationString);

            if (npc.getActionType() == null || npc.getActionType().equalsIgnoreCase("NONE") || npc.getActionValues().isEmpty()) {
                translations.sendMessage(player, "commands.npc.info.line-no-action");
            } else {
                translations.sendMessage(player, "commands.npc.info.line-action-type", "action_type", npc.getActionType());
                translations.sendMessage(player, "commands.npc.info.line-action-values", "count", String.valueOf(npc.getActionValues().size()));
                for (String value : npc.getActionValues()) {
                    translations.sendMessage(player, "commands.npc.info.line-action-value-entry", "value", value);
                }
            }
            player.sendMessage(" ");
        }, () -> translations.sendMessage(player, "commands.npc.delete.not-found", "id", id));
    }

    private void handleSetAlert(Player player, String[] args) {
        if (args.length < 3) {
            translations.sendMessage(player, "commands.npc.setalert.usage");
            return;
        }
        String id = args[1];
        String mode = args[2].toUpperCase();
        if (!List.of("NEVER", "EVERYONE", "FIRST").contains(mode)) {
            translations.sendMessage(player, "commands.npc.setalert.invalid-mode");
            return;
        }

        String text = "";
        if (args.length > 3) {
            text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        }

        if (!mode.equals("NEVER") && text.isEmpty()) {
            translations.sendMessage(player, "commands.npc.setalert.text-required");
            return;
        }

        npcManager.updateClickAlert(id, mode, text);
        translations.sendMessage(player, "commands.npc.setalert.success", "id", id, "mode", mode);
    }

    private void sendUsage(CommandSender sender) {
        translations.sendMessage(sender, "commands.npc.usage");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) return List.of();

        final String prefix = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            return Stream.of("sync", "criar", "deletar", "list", "tphere", "setskin", "setaction", "info", "setalert")
                    .filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Stream.of("deletar", "tphere", "setskin", "setaction", "info", "setalert").anyMatch(sub::equals)) {
                return npcManager.getAllNpcs().stream().map(NPC::getId)
                        .filter(id -> id.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setaction")) {
            return npcManager.getActionRegistry().getActionNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setalert")) {
            return Stream.of("NEVER", "EVERYONE", "FIRST")
                    .filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
        }

        return List.of();
    }
}
package br.com.realmmc.core.banner.command;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.banner.action.ActionType;
import br.com.realmmc.core.banner.action.BannerAction;
import br.com.realmmc.core.banner.action.ClickType;
import br.com.realmmc.core.banner.image.ImageMapConverter;
import br.com.realmmc.core.banner.manager.BannerManager;
import br.com.realmmc.core.banner.model.Banner;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.utils.JsonMessage;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BannerCommand implements CommandExecutor, TabCompleter {

    private final BannerManager bannerManager;
    private final ImageMapConverter imageMapConverter;
    private final TranslationsManager translations;
    private final String PERMISSION = "core.manager";

    public BannerCommand(BannerManager bannerManager, TranslationsManager translationsManager) {
        this.bannerManager = bannerManager;
        this.imageMapConverter = new ImageMapConverter(CoreAPI.getInstance().getPlugin());
        this.translations = translationsManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            translations.sendMessage(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            translations.sendNoPermissionMessage(player, "Gerente");
            return true;
        }
        if (args.length < 1) {
            sendHelpMessage(player);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "importar": handleImport(player, args); break;
            case "deletar": handleDelete(player, args); break;
            case "addaction": handleAddAction(player, args); break;
            case "remaction": handleRemoveAction(player, args); break;
            case "info": handleInfo(player, args); break;
            case "list": handleList(player); break;
            case "tp": handleTeleport(player, args); break;
            case "reload":
                bannerManager.despawnAllBanners();
                bannerManager.loadBanners();
                bannerManager.spawnAllBanners();
                translations.sendMessage(player, "commands.banner.success.reloaded");
                CoreAPI.getInstance().getSoundManager().playSuccess(player);
                break;
            default: sendHelpMessage(player); break;
        }
        return true;
    }

    private void sendHelpMessage(Player player) {
        CoreAPI.getInstance().getSoundManager().playError(player);
        player.sendMessage("§f");
        translations.sendMessage(player, "commands.banner.help.header", "system", "Banners");

        ConfigurationSection subcommandsSection = translations.getConfig().getConfigurationSection("commands.banner.help.subcommands");
        if (subcommandsSection != null) {
            for (String key : subcommandsSection.getKeys(false)) {
                String usage = subcommandsSection.getString(key + ".usage");
                String description = subcommandsSection.getString(key + ".description");
                if (usage != null && description != null) {
                    player.sendMessage(translations.getMessage("commands.banner.help.line-format", "usage", usage, "description", description));
                }
            }
        }

        player.sendMessage("§f");
        translations.sendMessage(player, "commands.banner.help.footer");
        player.sendMessage("§f");
    }

    private void handleList(Player player) {
        CoreAPI.getInstance().getSoundManager().playNotification(player);
        player.sendMessage("§f");
        translations.sendMessage(player, "commands.banner.list.header");

        if (bannerManager.getAllBanners().isEmpty()) {
            player.sendMessage(" §7- Nenhum banner criado.");
        } else {
            int index = 1;
            for (Banner banner : bannerManager.getAllBanners()) {
                String line = " &7" + index + ". &e" + banner.getId();
                JsonMessage json = JsonMessage.create(line);

                String[] hoverText = {
                        translations.getRawMessage("commands.banner.info.header", "id", banner.getId()),
                        translations.getRawMessage("commands.banner.info.image-file", "file_name", banner.getImageFile()),
                        translations.getRawMessage("commands.banner.info.dimensions", "width", String.valueOf(banner.getWidth()), "height", String.valueOf(banner.getHeight())),
                };
                json.withHover(hoverText);
                json.command("/banner info " + banner.getId());
                json.send(player);
                index++;
            }
        }
        player.sendMessage("§f");
    }

    private void handleTeleport(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.banner.usage", "usage", "banner tp <id>");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        Optional<Banner> bannerOpt = bannerManager.getBanner(args[1]);
        if (bannerOpt.isEmpty()) {
            translations.sendMessage(player, "commands.banner.error.not-found", "id", args[1]);
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        player.teleport(bannerOpt.get().getTopLeftLocation());
        translations.sendMessage(player, "commands.banner.success.teleported", "id", bannerOpt.get().getId());
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
    }

    private void handleImport(Player player, String[] args) {
        if (args.length != 5) {
            translations.sendMessage(player, "commands.banner.usage", "usage", "banner importar <id> <ficheiro.png> <largura> <altura>");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.isPassable()) {
            translations.sendMessage(player, "commands.banner.error.no-target-block");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        String id = args[1];
        String fileName = args[2];
        int width, height;
        try {
            width = Integer.parseInt(args[3]);
            height = Integer.parseInt(args[4]);
            if (width <= 0 || height <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            translations.sendMessage(player, "commands.banner.error.invalid-dimensions");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        if (bannerManager.getBanner(id).isPresent()) {
            translations.sendMessage(player, "commands.banner.error.id-exists", "id", id);
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        translations.sendMessage(player, "commands.banner.success.import-started");
        int[][] mapIds = imageMapConverter.convertImageToMapIds(fileName, width, height);
        if (mapIds == null) {
            translations.sendMessage(player, "commands.banner.error.image-conversion-failed");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        BlockFace facing = player.getTargetBlockFace(5);
        if (facing == null || facing == BlockFace.UP || facing == BlockFace.DOWN) {
            translations.sendMessage(player, "commands.banner.error.invalid-wall-face");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        bannerManager.createAndSaveBanner(id, fileName, width, height, mapIds, targetBlock.getLocation(), facing);
        bannerManager.despawnAllBanners();
        bannerManager.spawnAllBanners();
        translations.sendMessage(player, "commands.banner.success.created", "id", id);
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.banner.usage", "usage", "banner deletar <id>");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        String id = args[1];
        if (bannerManager.getBanner(id).isEmpty()) {
            translations.sendMessage(player, "commands.banner.error.not-found", "id", id);
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }
        bannerManager.deleteBanner(id);
        translations.sendMessage(player, "commands.banner.success.deleted", "id", id);
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
    }

    private void handleAddAction(Player player, String[] args) {
        if (args.length < 5) {
            translations.sendMessage(player, "commands.banner.usage", "usage", "banner addaction <id> <clique> <ação> <valor...>");
            player.sendMessage("§eTipos de Clique:§7 LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, ANY");
            player.sendMessage("§eTipos de Ação:§7 COMMAND, MESSAGE, OPEN_MENU");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        Optional<Banner> bannerOpt = bannerManager.getBanner(args[1]);
        if (bannerOpt.isEmpty()) {
            translations.sendMessage(player, "commands.banner.error.not-found", "id", args[1]);
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        ClickType clickType = ClickType.fromString(args[2]);
        if (clickType == null) {
            translations.sendMessage(player, "commands.banner.error.invalid-click-type");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        ActionType actionType = ActionType.fromString(args[3]);
        if (actionType == null) {
            translations.sendMessage(player, "commands.banner.error.invalid-action-type");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        String value = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        Banner banner = bannerOpt.get();
        banner.addAction(clickType, new BannerAction(actionType, value));
        bannerManager.saveBanners();

        translations.sendMessage(player, "commands.banner.success.action-added", "id", banner.getId());
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
    }

    private void handleRemoveAction(Player player, String[] args) {
        if (args.length != 3) {
            translations.sendMessage(player, "commands.banner.usage", "usage", "banner remaction <id> <clique>");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        Optional<Banner> bannerOpt = bannerManager.getBanner(args[1]);
        if (bannerOpt.isEmpty()) {
            translations.sendMessage(player, "commands.banner.error.not-found", "id", args[1]);
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        ClickType clickType = ClickType.fromString(args[2]);
        if (clickType == null) {
            translations.sendMessage(player, "commands.banner.error.invalid-click-type");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        Banner banner = bannerOpt.get();
        banner.removeAction(clickType);
        bannerManager.saveBanners();

        translations.sendMessage(player, "commands.banner.success.action-removed", "id", banner.getId(), "click_type", clickType.name());
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length != 2) {
            translations.sendMessage(player, "commands.banner.usage", "usage", "banner info <id>");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        Optional<Banner> bannerOpt = bannerManager.getBanner(args[1]);
        if (bannerOpt.isEmpty()) {
            translations.sendMessage(player, "commands.banner.error.not-found", "id", args[1]);
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        Banner banner = bannerOpt.get();
        CoreAPI.getInstance().getSoundManager().playClick(player);

        player.sendMessage("§f");
        translations.sendMessage(player, "commands.banner.info.header", "id", banner.getId());
        translations.sendMessage(player, "commands.banner.info.image-file", "file_name", banner.getImageFile());
        translations.sendMessage(player, "commands.banner.info.dimensions", "width", String.valueOf(banner.getWidth()), "height", String.valueOf(banner.getHeight()));
        Location loc = banner.getTopLeftLocation();
        translations.sendMessage(player, "commands.banner.info.location", "world", loc.getWorld().getName(), "x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ()));

        player.sendMessage("§f");
        translations.sendMessage(player, "commands.banner.info.actions-header");
        if (banner.getActions().isEmpty()) {
            translations.sendMessage(player, "commands.banner.info.actions-none");
        } else {
            banner.getActions().forEach((click, action) -> {
                translations.sendMessage(player, "commands.banner.info.actions-line", "click_type", click.name(), "action_type", action.type().name(), "value", action.value());
            });
        }
        player.sendMessage("§f");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String prefix = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            return Stream.of("importar", "deletar", "addaction", "remaction", "info", "list", "tp", "reload")
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("deletar", "info", "addaction", "remaction", "tp").contains(subCommand)) {
                return bannerManager.getAllBanners().stream()
                        .map(Banner::getId)
                        .filter(id -> id.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("addaction") || subCommand.equals("remaction")) {
                return Stream.of(ClickType.values())
                        .map(Enum::name)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("addaction")) {
                return Stream.of(ActionType.values())
                        .map(Enum::name)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}
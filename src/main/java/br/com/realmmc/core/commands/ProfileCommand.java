package br.com.realmmc.core.commands;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.profile.ProfileGUI;
import br.com.realmmc.core.managers.TranslationsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ProfileCommand implements CommandExecutor {

    private final TranslationsManager translations;

    public ProfileCommand() {
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            translations.sendMessage(sender, "general.player-only");
            return true;
        }

        CoreAPI.getInstance().getSoundManager().playClick(player);

        new ProfileGUI(player).open();
        return true;
    }
}
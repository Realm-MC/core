package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.ColorAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TranslationsManager {

    private final Main plugin;
    private final Logger logger;
    private final Map<String, String> messages = new HashMap<>();
    private final String defaultLocale = "pt_BR";
    private final String logPrefix = "[Core] ";

    public TranslationsManager(Main plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadMessages();
    }

    private void loadMessages() {
        File langFile = new File(plugin.getDataFolder(), "translations/" + defaultLocale + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("translations/" + defaultLocale + ".yml", false);
        }

        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        Reader defaultConfigStream = new InputStreamReader(plugin.getResource("translations/" + defaultLocale + ".yml"), StandardCharsets.UTF_8);
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
        langConfig.setDefaults(defaultConfig);

        messages.clear();
        loadSection(langConfig, "");
    }

    private void loadSection(ConfigurationSection config, String path) {
        for (String key : config.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (config.isConfigurationSection(key)) {
                loadSection(config.getConfigurationSection(key), fullPath);
            } else {
                messages.put(fullPath, config.getString(key));
            }
        }
    }

    // --- MÉTODO ADICIONADO PARA CORRIGIR O ERRO ---
    public void sendNoPermissionMessage(CommandSender sender, String requiredGroup) {
        sendMessage(sender, "general.no-permission", "group", requiredGroup);
        if (sender instanceof Player) {
            CoreAPI.getInstance().getSoundManager().playError((Player) sender);
        }
    }

    public String getRawMessage(String key, String... replacements) {
        String message = messages.getOrDefault(key, "&c[Chave Faltando: " + key + "]");
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return message;
    }

    public String getMessage(String key, String... replacements) {
        return ColorAPI.format(getRawMessage(key, replacements));
    }

    public void sendMessage(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(getMessage(key, replacements));
    }

    public void log(Level level, String key, String... replacements) {
        logger.log(level, logPrefix + getRawMessage(key, replacements));
    }

    public void log(Level level, String key, Throwable throwable, String... replacements) {
        logger.log(level, logPrefix + getRawMessage(key, replacements), throwable);
    }
}
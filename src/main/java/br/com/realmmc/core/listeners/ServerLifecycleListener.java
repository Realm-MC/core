package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.npc.NPCListener;
import br.com.realmmc.core.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class ServerLifecycleListener implements Listener {

    private final Main plugin;
    private boolean hasLoadedNpcs = false;

    public ServerLifecycleListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (hasLoadedNpcs) {
            return;
        }
        this.hasLoadedNpcs = true;

        plugin.getLogger().info("Servidor completamente carregado. Iniciando carregamento de NPCs...");

        NPCManager npcManager = plugin.getNpcManager();
        if (npcManager != null && Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            npcManager.loadAndSpawnAll();
            new NPCListener(plugin);
            plugin.getLogger().info("NPCs carregados com sucesso.");
        } else {
            plugin.getLogger().info("Sistema de NPCs desativado pois o plugin Citizens não foi encontrado.");
            PluginCommand npcCommand = plugin.getCommand("npcs");
            if (npcCommand != null) {
                npcCommand.setExecutor((sender, command, label, args) -> {
                    sender.sendMessage("§cO sistema de NPCs está desativado neste servidor.");
                    return true;
                });
            }
        }
    }
}
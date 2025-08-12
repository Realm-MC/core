package br.com.realmmc.core.modules;

import br.com.realmmc.core.Main;

import java.util.HashSet;
import java.util.Set;

public class ModuleManager {

    private final Main plugin;
    private final Set<SystemType> claimedSystems = new HashSet<>();

    public ModuleManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Permite que outro plugin reivindique a responsabilidade por um sistema padrão do Core.
     * Deve ser chamado durante o onEnable() do plugin customizado.
     *
     * @param type O tipo de sistema (SCOREBOARD, CHAT, ou TAGS) a ser reivindicado.
     */
    public void claimSystem(SystemType type) {
        claimedSystems.add(type);
        plugin.getLogger().info("O sistema " + type.name() + " foi reivindicado por um plugin customizado. A versão padrão do Core não será ativada.");
    }

    /**
     * Verifica se um sistema já foi reivindicado por outro plugin.
     *
     * @param type O tipo de sistema a ser verificado.
     * @return true se o sistema foi reivindicado, false caso contrário.
     */
    public boolean isClaimed(SystemType type) {
        return claimedSystems.contains(type);
    }
}
package br.com.realmmc.core.banner.listener;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.banner.action.BannerAction;
import br.com.realmmc.core.banner.action.ClickType;
import br.com.realmmc.core.banner.manager.BannerManager;
import br.com.realmmc.core.banner.model.Banner;
import br.com.realmmc.core.gui.GuiManager;
import br.com.realmmc.core.utils.ColorAPI;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.Optional;

public class BannerListener implements Listener {

    private final BannerManager bannerManager;
    private final GuiManager guiManager;

    public BannerListener(Main plugin) {
        this.bannerManager = plugin.getBannerManager();
        this.guiManager = plugin.getGuiManager();
    }

    @EventHandler
    public void onBannerRightClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame) || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        List<MetadataValue> metadata = event.getRightClicked().getMetadata("realm_banner_id");
        if (metadata.isEmpty()) return;

        String bannerId = metadata.get(0).asString();
        bannerManager.getBanner(bannerId).ifPresent(banner -> {
            event.setCancelled(true);
            Player player = event.getPlayer();
            ClickType clickType = player.isSneaking() ? ClickType.SHIFT_RIGHT : ClickType.RIGHT;
            banner.getAction(clickType).ifPresent(action -> executeAction(player, action));
        });
    }

    @EventHandler
    public void onBannerLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame) || !(event.getDamager() instanceof Player)) {
            return;
        }
        List<MetadataValue> metadata = event.getEntity().getMetadata("realm_banner_id");
        if (metadata.isEmpty()) return;

        String bannerId = metadata.get(0).asString();
        bannerManager.getBanner(bannerId).ifPresent(banner -> {
            event.setCancelled(true);
            Player player = (Player) event.getDamager();
            ClickType clickType = player.isSneaking() ? ClickType.SHIFT_LEFT : ClickType.LEFT;
            banner.getAction(clickType).ifPresent(action -> executeAction(player, action));
        });
    }

    private void executeAction(Player player, BannerAction action) {
        CoreAPI.getInstance().getSoundManager().playClick(player);
        switch (action.type()) {
            case COMMAND:
                player.performCommand(action.value());
                break;
            case MESSAGE:
                player.sendMessage(ColorAPI.format(action.value()));
                break;
            case OPEN_MENU:
                guiManager.openGui(player, action.value());
                break;
        }
    }
}
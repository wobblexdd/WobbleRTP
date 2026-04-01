package me.wobble.wobblertp.listener;

import me.wobble.wobblertp.gui.RtpGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GuiListener implements Listener {

    private final RtpGui rtpGui;

    public GuiListener(RtpGui rtpGui) {
        this.rtpGui = rtpGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!rtpGui.isRtpMenu(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        rtpGui.handleClick(player, event.getRawSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (rtpGui.isRtpMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}

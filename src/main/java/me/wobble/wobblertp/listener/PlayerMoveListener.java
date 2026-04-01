package me.wobble.wobblertp.listener;

import me.wobble.wobblertp.manager.CountdownManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PlayerMoveListener implements Listener {

    private final CountdownManager countdownManager;

    public PlayerMoveListener(CountdownManager countdownManager) {
        this.countdownManager = countdownManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        countdownManager.handleMovement(event.getPlayer(), event.getFrom(), event.getTo());
    }
}

package me.wobble.wobblertp.listener;

import me.wobble.wobblertp.manager.CountdownManager;
import me.wobble.wobblertp.service.RtpService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {

    private final RtpService rtpService;
    private final CountdownManager countdownManager;

    public PlayerQuitListener(RtpService rtpService, CountdownManager countdownManager) {
        this.rtpService = rtpService;
        this.countdownManager = countdownManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        countdownManager.cancel(event.getPlayer().getUniqueId(), false, null);
        rtpService.clearSearchState(event.getPlayer().getUniqueId());
    }
}

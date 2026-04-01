package me.wobble.wobblertp.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import me.wobble.wobblertp.WobbleRTP;
import me.wobble.wobblertp.model.PendingTeleport;
import me.wobble.wobblertp.model.RtpWorldType;
import me.wobble.wobblertp.util.ChatUtil;
import me.wobble.wobblertp.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class CountdownManager {

    private final WobbleRTP plugin;
    private final ConfigurationManager configurationManager;
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    public CountdownManager(WobbleRTP plugin, ConfigurationManager configurationManager) {
        this.plugin = plugin;
        this.configurationManager = configurationManager;
    }

    public boolean hasPending(UUID playerId) {
        return pendingTeleports.containsKey(playerId);
    }

    public boolean startCountdown(Player player, RtpWorldType worldType, Location destination,
                                  Consumer<PendingTeleport> completionHandler) {
        int countdownSeconds = configurationManager.getSettings().countdownSeconds();
        if (countdownSeconds <= 0) {
            completionHandler.accept(new PendingTeleport(player.getUniqueId(), worldType, player.getLocation().clone(),
                    destination.clone(), 0));
            return true;
        }

        PendingTeleport pendingTeleport = new PendingTeleport(
                player.getUniqueId(),
                worldType,
                player.getLocation().clone(),
                destination.clone(),
                countdownSeconds
        );
        PendingTeleport existing = pendingTeleports.putIfAbsent(player.getUniqueId(), pendingTeleport);
        if (existing != null) {
            return false;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickCountdown(pendingTeleport, completionHandler), 0L, 20L);
        pendingTeleport.setTask(task);
        return true;
    }

    public void handleMovement(Player player, Location from, Location to) {
        if (!configurationManager.getSettings().cancelOnMove()) {
            return;
        }
        if (player.hasPermission("wobble.rtp.bypass.move")) {
            return;
        }
        if (!hasPending(player.getUniqueId())) {
            return;
        }
        if (from.getWorld() != to.getWorld()) {
            cancel(player.getUniqueId(), true, player);
            return;
        }
        double deltaX = Math.abs(from.getX() - to.getX());
        double deltaY = Math.abs(from.getY() - to.getY());
        double deltaZ = Math.abs(from.getZ() - to.getZ());
        if (deltaX < 0.001D && deltaY < 0.001D && deltaZ < 0.001D) {
            return;
        }
        cancel(player.getUniqueId(), true, player);
    }

    public void cancel(UUID playerId, boolean notifyPlayer, Player player) {
        PendingTeleport pendingTeleport = pendingTeleports.remove(playerId);
        if (pendingTeleport == null) {
            return;
        }

        if (pendingTeleport.getTask() != null) {
            pendingTeleport.getTask().cancel();
        }

        if (notifyPlayer && player != null && player.isOnline()) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "movement-cancelled"));
            player.sendActionBar(ChatUtil.component(configurationManager.getMessage("movement-cancelled")));
            SoundUtil.play(player, configurationManager.getSettings().sound("movement-cancelled"));
        }
    }

    public void cancelAll(boolean notify) {
        for (UUID playerId : pendingTeleports.keySet()) {
            Player player = notify ? Bukkit.getPlayer(playerId) : null;
            cancel(playerId, notify, player);
        }
    }

    private void tickCountdown(PendingTeleport pendingTeleport, Consumer<PendingTeleport> completionHandler) {
        Player player = Bukkit.getPlayer(pendingTeleport.getPlayerId());
        if (player == null || !player.isOnline()) {
            cancel(pendingTeleport.getPlayerId(), false, null);
            return;
        }

        int remainingSeconds = pendingTeleport.getRemainingSeconds();
        if (remainingSeconds <= 0) {
            pendingTeleports.remove(pendingTeleport.getPlayerId());
            if (pendingTeleport.getTask() != null) {
                pendingTeleport.getTask().cancel();
            }
            completionHandler.accept(pendingTeleport);
            return;
        }

        player.sendActionBar(ChatUtil.component(configurationManager.getMessage("teleport-countdown"),
                ChatUtil.placeholder("seconds", remainingSeconds)));
        SoundUtil.play(player, configurationManager.getSettings().sound("countdown-tick"));
        pendingTeleport.decrement();
    }
}

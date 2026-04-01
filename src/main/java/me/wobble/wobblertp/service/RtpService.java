package me.wobble.wobblertp.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import me.wobble.wobblertp.WobbleRTP;
import me.wobble.wobblertp.manager.ConfigurationManager;
import me.wobble.wobblertp.manager.CooldownManager;
import me.wobble.wobblertp.manager.CountdownManager;
import me.wobble.wobblertp.model.PendingTeleport;
import me.wobble.wobblertp.model.PluginSettings;
import me.wobble.wobblertp.model.RtpWorldType;
import me.wobble.wobblertp.util.ChatUtil;
import me.wobble.wobblertp.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class RtpService {

    private final WobbleRTP plugin;
    private final ConfigurationManager configurationManager;
    private final CooldownManager cooldownManager;
    private final CountdownManager countdownManager;
    private final SafeLocationFinder safeLocationFinder;
    private final ConcurrentMap<UUID, UUID> activeSearches = new ConcurrentHashMap<>();

    public RtpService(WobbleRTP plugin, ConfigurationManager configurationManager, CooldownManager cooldownManager,
                      CountdownManager countdownManager, SafeLocationFinder safeLocationFinder) {
        this.plugin = plugin;
        this.configurationManager = configurationManager;
        this.cooldownManager = cooldownManager;
        this.countdownManager = countdownManager;
        this.safeLocationFinder = safeLocationFinder;
    }

    public void beginTeleport(Player player, RtpWorldType worldType) {
        if (!player.hasPermission("wobble.rtp.use")) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "no-permission"));
            return;
        }

        PluginSettings settings = configurationManager.getSettings();
        UUID playerId = player.getUniqueId();

        if (countdownManager.hasPending(playerId)) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "already-teleporting"));
            return;
        }

        if (!player.hasPermission("wobble.rtp.bypass.cooldown")) {
            long remaining = cooldownManager.getRemainingSeconds(playerId);
            if (remaining > 0L) {
                player.sendMessage(ChatUtil.prefixed(configurationManager, "cooldown",
                        ChatUtil.placeholder("time", ChatUtil.formatDuration(remaining))));
                return;
            }
        }

        String configuredWorld = settings.worldName(worldType);
        World world = Bukkit.getWorld(configuredWorld);
        if (world == null) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "invalid-world",
                    ChatUtil.placeholder("world", worldType.getDisplayName())));
            return;
        }

        UUID searchToken = UUID.randomUUID();
        UUID existingSearch = activeSearches.putIfAbsent(playerId, searchToken);
        if (existingSearch != null) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "already-teleporting"));
            return;
        }

        player.sendMessage(ChatUtil.prefixed(configurationManager, "searching",
                ChatUtil.placeholder("world", worldType.getDisplayName())));
        SoundUtil.play(player, settings.sound("searching"));

        CompletableFuture<Optional<Location>> future = safeLocationFinder.findSafeLocation(world, worldType);
        future.whenComplete((location, throwable) -> Bukkit.getScheduler().runTask(plugin,
                () -> handleSearchResult(playerId, searchToken, worldType, location, throwable)));
    }

    public void clearSearchState(UUID playerId) {
        activeSearches.remove(playerId);
    }

    public void shutdown() {
        activeSearches.clear();
    }

    private void handleSearchResult(UUID playerId, UUID searchToken, RtpWorldType worldType,
                                    Optional<Location> location, Throwable throwable) {
        if (!plugin.isEnabled()) {
            activeSearches.remove(playerId, searchToken);
            return;
        }
        if (!activeSearches.remove(playerId, searchToken)) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }

        if (throwable != null || location == null || location.isEmpty()) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "teleport-failed"));
            SoundUtil.play(player, configurationManager.getSettings().sound("teleport-failed"));
            return;
        }

        boolean started = countdownManager.startCountdown(player, worldType, location.get(),
                pendingTeleport -> completeTeleport(playerId, pendingTeleport));
        if (!started) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "already-teleporting"));
        }
    }

    private void completeTeleport(UUID playerId, PendingTeleport pendingTeleport) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }

        Location destination = pendingTeleport.getDestination().clone();
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());

        if (!safeLocationFinder.isSafeDestination(destination, pendingTeleport.getWorldType())) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "teleport-failed"));
            SoundUtil.play(player, configurationManager.getSettings().sound("teleport-failed"));
            return;
        }

        boolean teleported = player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (!teleported) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "teleport-failed"));
            SoundUtil.play(player, configurationManager.getSettings().sound("teleport-failed"));
            return;
        }

        if (!player.hasPermission("wobble.rtp.bypass.cooldown")) {
            cooldownManager.startCooldown(playerId, configurationManager.getSettings().cooldownSeconds());
        }

        player.sendMessage(ChatUtil.prefixed(configurationManager, "teleport-success",
                ChatUtil.placeholder("world", pendingTeleport.getWorldType().getDisplayName()),
                ChatUtil.placeholder("x", destination.getBlockX()),
                ChatUtil.placeholder("z", destination.getBlockZ())));
        SoundUtil.play(player, configurationManager.getSettings().sound("teleport-success"));
    }
}

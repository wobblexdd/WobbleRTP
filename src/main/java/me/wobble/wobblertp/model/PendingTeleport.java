package me.wobble.wobblertp.model;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

public final class PendingTeleport {

    private final UUID playerId;
    private final RtpWorldType worldType;
    private final Location origin;
    private final Location destination;
    private int remainingSeconds;
    private BukkitTask task;

    public PendingTeleport(UUID playerId, RtpWorldType worldType, Location origin, Location destination, int remainingSeconds) {
        this.playerId = playerId;
        this.worldType = worldType;
        this.origin = origin;
        this.destination = destination;
        this.remainingSeconds = remainingSeconds;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public RtpWorldType getWorldType() {
        return worldType;
    }

    public Location getOrigin() {
        return origin;
    }

    public Location getDestination() {
        return destination;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void decrement() {
        remainingSeconds--;
    }

    public BukkitTask getTask() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }
}

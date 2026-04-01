package me.wobble.wobblertp.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public long getRemainingSeconds(UUID playerId) {
        Long expiry = cooldowns.get(playerId);
        if (expiry == null) {
            return 0L;
        }

        long remainingMillis = expiry - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            cooldowns.remove(playerId);
            return 0L;
        }

        return Math.max(1L, (long) Math.ceil(remainingMillis / 1000.0D));
    }

    public void startCooldown(UUID playerId, int seconds) {
        if (seconds <= 0) {
            cooldowns.remove(playerId);
            return;
        }
        cooldowns.put(playerId, System.currentTimeMillis() + seconds * 1000L);
    }
}

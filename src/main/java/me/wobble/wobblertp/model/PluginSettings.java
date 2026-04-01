package me.wobble.wobblertp.model;

import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Biome;

public record PluginSettings(
        int minDistance,
        int maxDistance,
        int teleportRetries,
        int loadedChunkAttempts,
        int netherMaxY,
        boolean waterUnsafe,
        int countdownSeconds,
        boolean cancelOnMove,
        int cooldownSeconds,
        Map<RtpWorldType, String> worlds,
        Set<Biome> disabledBiomes,
        Set<Material> dangerousBlocks,
        boolean soundsEnabled,
        Map<String, ConfiguredSound> sounds
) {

    public String worldName(RtpWorldType worldType) {
        return worlds.get(worldType);
    }

    public ConfiguredSound sound(String key) {
        if (!soundsEnabled) {
            return ConfiguredSound.disabled();
        }
        return sounds.getOrDefault(key, ConfiguredSound.disabled());
    }
}

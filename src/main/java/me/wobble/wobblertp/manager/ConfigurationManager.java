package me.wobble.wobblertp.manager;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.wobble.wobblertp.WobbleRTP;
import me.wobble.wobblertp.model.ConfiguredSound;
import me.wobble.wobblertp.model.PluginSettings;
import me.wobble.wobblertp.model.RtpWorldType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigurationManager {

    private final WobbleRTP plugin;
    private FileConfiguration messagesConfiguration;
    private PluginSettings settings;

    public ConfigurationManager(WobbleRTP plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        saveMessagesFile();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        saveMessagesFile();
        this.messagesConfiguration = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        this.settings = loadSettings(plugin.getConfig());
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public String getMessage(String path) {
        return messagesConfiguration.getString(path, "");
    }

    public List<String> getMessageList(String path) {
        return messagesConfiguration.getStringList(path);
    }

    private void saveMessagesFile() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File messagesFile = new File(dataFolder, "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    private PluginSettings loadSettings(FileConfiguration config) {
        Map<RtpWorldType, String> worlds = new EnumMap<>(RtpWorldType.class);
        worlds.put(RtpWorldType.OVERWORLD, config.getString("worlds.overworld", "world"));
        worlds.put(RtpWorldType.NETHER, config.getString("worlds.nether", "world_nether"));
        worlds.put(RtpWorldType.THE_END, config.getString("worlds.the_end", "world_the_end"));

        Set<Biome> disabledBiomes = new HashSet<>();
        for (String biomeName : config.getStringList("disabled-biomes")) {
            Biome biome = parseBiome(biomeName);
            if (biome != null) {
                disabledBiomes.add(biome);
            } else {
                plugin.getLogger().warning("Ignoring unknown biome in config: " + biomeName);
            }
        }

        Set<Material> dangerousBlocks = new HashSet<>();
        for (String materialName : config.getStringList("dangerous-blocks")) {
            try {
                dangerousBlocks.add(Material.valueOf(materialName.toUpperCase()));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring unknown dangerous block in config: " + materialName);
            }
        }

        Map<String, ConfiguredSound> sounds = new HashMap<>();
        for (String key : List.of("gui-open", "searching", "countdown-tick", "teleport-success", "teleport-failed", "movement-cancelled")) {
            sounds.put(key, loadSound(config, key));
        }

        int minDistance = Math.max(0, config.getInt("min-distance", 800));
        int configuredMaxDistance = config.getInt("max-distance", 6000);
        int maxDistance = Math.max(minDistance, configuredMaxDistance);
        if (configuredMaxDistance < minDistance) {
            plugin.getLogger().warning("max-distance was lower than min-distance; using min-distance value instead.");
        }

        return new PluginSettings(
                minDistance,
                maxDistance,
                Math.max(1, config.getInt("teleport-retries", 48)),
                Math.max(0, config.getInt("search.loaded-chunk-attempts", 8)),
                Math.max(5, config.getInt("search.nether-max-y", 120)),
                config.getBoolean("safety.treat-water-as-unsafe", true),
                Math.max(0, config.getInt("countdown.seconds", 5)),
                config.getBoolean("countdown.cancel-on-move", true),
                Math.max(0, config.getInt("cooldown.seconds", 90)),
                worlds,
                disabledBiomes,
                dangerousBlocks,
                config.getBoolean("sounds.enabled", true),
                sounds
        );
    }

    private ConfiguredSound loadSound(FileConfiguration config, String key) {
        if (!config.getBoolean("sounds.enabled", true)) {
            return ConfiguredSound.disabled();
        }

        String path = "sounds." + key;
        String soundName = config.getString(path + ".name");
        if (soundName == null || soundName.isBlank()) {
            return ConfiguredSound.disabled();
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) config.getDouble(path + ".volume", 1.0D);
            float pitch = (float) config.getDouble(path + ".pitch", 1.0D);
            return new ConfiguredSound(sound, volume, pitch);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Ignoring unknown sound in config: " + soundName);
            return ConfiguredSound.disabled();
        }
    }

    private Biome parseBiome(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        NamespacedKey key = input.contains(":")
                ? NamespacedKey.fromString(input.toLowerCase())
                : NamespacedKey.minecraft(input.toLowerCase());
        return key == null ? null : Registry.BIOME.get(key);
    }
}

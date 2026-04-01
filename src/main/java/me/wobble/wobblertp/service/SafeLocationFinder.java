package me.wobble.wobblertp.service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import me.wobble.wobblertp.WobbleRTP;
import me.wobble.wobblertp.manager.ConfigurationManager;
import me.wobble.wobblertp.model.PluginSettings;
import me.wobble.wobblertp.model.RtpWorldType;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class SafeLocationFinder {

    private final WobbleRTP plugin;
    private final ConfigurationManager configurationManager;
    private final Executor searchExecutor;

    public SafeLocationFinder(WobbleRTP plugin, ConfigurationManager configurationManager, Executor searchExecutor) {
        this.plugin = plugin;
        this.configurationManager = configurationManager;
        this.searchExecutor = searchExecutor;
    }

    public CompletableFuture<Optional<Location>> findSafeLocation(World world, RtpWorldType worldType) {
        PluginSettings settings = configurationManager.getSettings();
        SearchBounds bounds = createBounds(world.getWorldBorder(), settings);
        if (!bounds.valid()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        Set<Long> attemptedChunks = ConcurrentHashMap.newKeySet();
        return tryLoadedChunks(world, worldType, bounds, attemptedChunks, settings.loadedChunkAttempts())
                .thenCompose(found -> found.isPresent()
                        ? CompletableFuture.completedFuture(found)
                        : tryAsyncChunks(world, worldType, bounds, attemptedChunks, settings.teleportRetries()));
    }

    public boolean isSafeDestination(Location destination, RtpWorldType worldType) {
        World world = destination.getWorld();
        if (world == null) {
            return false;
        }

        int x = destination.getBlockX();
        int y = destination.getBlockY();
        int z = destination.getBlockZ();
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            return false;
        }

        Block ground = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        return isSpawnSpaceSafe(world, ground, feet, head, worldType);
    }

    private CompletableFuture<Optional<Location>> tryLoadedChunks(World world, RtpWorldType worldType, SearchBounds bounds,
                                                                  Set<Long> attemptedChunks, int attempts) {
        if (attempts <= 0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return callSync(() -> {
            for (int attempt = 0; attempt < attempts; attempt++) {
                Candidate candidate = createCandidate(bounds, attemptedChunks);
                if (candidate == null) {
                    break;
                }
                if (!world.isChunkLoaded(candidate.chunkX(), candidate.chunkZ())) {
                    continue;
                }

                Optional<Location> safeLocation = validateCandidate(world, worldType, candidate.x(), candidate.z());
                if (safeLocation.isPresent()) {
                    return safeLocation;
                }
            }
            return Optional.empty();
        });
    }

    private CompletableFuture<Optional<Location>> tryAsyncChunks(World world, RtpWorldType worldType, SearchBounds bounds,
                                                                 Set<Long> attemptedChunks, int remainingAttempts) {
        if (remainingAttempts <= 0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.supplyAsync(() -> createCandidate(bounds, attemptedChunks), searchExecutor)
                .thenCompose(candidate -> {
                    if (candidate == null) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }

                    return world.getChunkAtAsync(candidate.chunkX(), candidate.chunkZ(), true)
                            .thenCompose(chunk -> callSync(() -> validateCandidate(world, worldType, candidate.x(), candidate.z())))
                            .thenCompose(found -> found.isPresent()
                                    ? CompletableFuture.completedFuture(found)
                                    : tryAsyncChunks(world, worldType, bounds, attemptedChunks, remainingAttempts - 1));
                });
    }

    private Optional<Location> validateCandidate(World world, RtpWorldType worldType, int x, int z) {
        if (worldType == RtpWorldType.NETHER) {
            return validateNetherCandidate(world, x, z);
        }
        return validateSurfaceCandidate(world, worldType, x, z);
    }

    private Optional<Location> validateSurfaceCandidate(World world, RtpWorldType worldType, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int feetY = highestY + 1;
        if (feetY <= world.getMinHeight() || feetY + 1 >= world.getMaxHeight()) {
            return Optional.empty();
        }

        Block ground = world.getBlockAt(x, feetY - 1, z);
        Block feet = world.getBlockAt(x, feetY, z);
        Block head = world.getBlockAt(x, feetY + 1, z);
        if (!isSpawnSpaceSafe(world, ground, feet, head, worldType)) {
            return Optional.empty();
        }

        return Optional.of(buildLocation(world, x, feetY, z));
    }

    private Optional<Location> validateNetherCandidate(World world, int x, int z) {
        int maxY = Math.min(configurationManager.getSettings().netherMaxY(), world.getMaxHeight() - 2);
        for (int y = maxY; y > world.getMinHeight(); y--) {
            Block ground = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (isSpawnSpaceSafe(world, ground, feet, head, RtpWorldType.NETHER)) {
                return Optional.of(buildLocation(world, x, y, z));
            }
        }
        return Optional.empty();
    }

    private boolean isSpawnSpaceSafe(World world, Block ground, Block feet, Block head, RtpWorldType worldType) {
        PluginSettings settings = configurationManager.getSettings();
        if (settings.disabledBiomes().contains(feet.getBiome())) {
            return false;
        }
        if (feet.getY() <= world.getMinHeight()) {
            return false;
        }
        if (worldType == RtpWorldType.NETHER && feet.getY() >= settings.netherMaxY()) {
            return false;
        }
        if (!ground.getType().isSolid() || ground.isPassable()) {
            return false;
        }
        if (isDangerous(settings, ground.getType()) || isDangerous(settings, feet.getType()) || isDangerous(settings, head.getType())) {
            return false;
        }
        if (isLava(ground.getType()) || isLava(feet.getType()) || isLava(head.getType())) {
            return false;
        }
        if (settings.waterUnsafe() && (isWater(ground.getType()) || isWater(feet.getType()) || isWater(head.getType()))) {
            return false;
        }
        if (!isPassableSpawnBlock(feet.getType(), feet) || !isPassableSpawnBlock(head.getType(), head)) {
            return false;
        }
        if (worldType == RtpWorldType.NETHER && ground.getType() == Material.BEDROCK && feet.getY() > settings.netherMaxY() - 8) {
            return false;
        }
        if (touchesDangerousFluid(feet) || touchesDangerousFluid(head)) {
            return false;
        }
        return true;
    }

    private boolean isPassableSpawnBlock(Material material, Block block) {
        if (material == Material.LAVA || material == Material.FIRE || material == Material.SOUL_FIRE) {
            return false;
        }
        return block.isPassable() || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR
                || material == Material.WATER || material == Material.BUBBLE_COLUMN;
    }

    private boolean isDangerous(PluginSettings settings, Material material) {
        return settings.dangerousBlocks().contains(material);
    }

    private boolean isWater(Material material) {
        return material == Material.WATER || material == Material.BUBBLE_COLUMN || material == Material.KELP
                || material == Material.KELP_PLANT || material == Material.SEAGRASS || material == Material.TALL_SEAGRASS;
    }

    private boolean isLava(Material material) {
        return material == Material.LAVA;
    }

    private boolean touchesDangerousFluid(Block block) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN}) {
            Material nearby = block.getRelative(face).getType();
            if (isLava(nearby)) {
                return true;
            }
        }
        return false;
    }

    private Location buildLocation(World world, int x, int y, int z) {
        return new Location(world, x + 0.5D, y, z + 0.5D);
    }

    private Candidate createCandidate(SearchBounds bounds, Set<Long> attemptedChunks) {
        for (int attempts = 0; attempts < 16; attempts++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            int distance = ThreadLocalRandom.current().nextInt(bounds.minDistance(), bounds.maxDistance() + 1);
            int x = (int) Math.round(bounds.centerX() + Math.cos(angle) * distance);
            int z = (int) Math.round(bounds.centerZ() + Math.sin(angle) * distance);
            if (!bounds.contains(x + 0.5D, z + 0.5D)) {
                continue;
            }

            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
            if (!attemptedChunks.add(chunkKey)) {
                continue;
            }
            return new Candidate(x, z, chunkX, chunkZ);
        }
        return null;
    }

    private SearchBounds createBounds(WorldBorder border, PluginSettings settings) {
        double halfBorder = Math.max(0D, border.getSize() / 2.0D - 16.0D);
        int maxDistance = Math.min(settings.maxDistance(), (int) Math.floor(halfBorder));
        int minDistance = Math.min(settings.minDistance(), maxDistance);
        return new SearchBounds(border.getCenter().getX(), border.getCenter().getZ(), minDistance, maxDistance);
    }

    private <T> CompletableFuture<T> callSync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private record Candidate(int x, int z, int chunkX, int chunkZ) {
    }

    private record SearchBounds(double centerX, double centerZ, int minDistance, int maxDistance) {
        boolean valid() {
            return maxDistance > 0 && maxDistance >= minDistance;
        }

        boolean contains(double x, double z) {
            return x >= centerX - maxDistance && x <= centerX + maxDistance
                    && z >= centerZ - maxDistance && z <= centerZ + maxDistance;
        }
    }
}

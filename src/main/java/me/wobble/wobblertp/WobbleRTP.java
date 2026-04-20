package me.wobble.wobblertp;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import me.wobble.wobblertp.command.RtpCommand;
import me.wobble.wobblertp.gui.RtpGui;
import me.wobble.wobblertp.listener.GuiListener;
import me.wobble.wobblertp.listener.PlayerKickListener;
import me.wobble.wobblertp.listener.PlayerMoveListener;
import me.wobble.wobblertp.listener.PlayerQuitListener;
import me.wobble.wobblertp.manager.ConfigurationManager;
import me.wobble.wobblertp.manager.CooldownManager;
import me.wobble.wobblertp.manager.CountdownManager;
import me.wobble.wobblertp.service.RtpService;
import me.wobble.wobblertp.service.SafeLocationFinder;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WobbleRTP extends JavaPlugin {

    private ExecutorService searchExecutor;
    private ConfigurationManager configurationManager;
    private CountdownManager countdownManager;
    private RtpService rtpService;

    @Override
    public void onEnable() {
        this.searchExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                new SearchThreadFactory()
        );

        this.configurationManager = new ConfigurationManager(this);
        CooldownManager cooldownManager = new CooldownManager();
        this.countdownManager = new CountdownManager(this, configurationManager);
        SafeLocationFinder safeLocationFinder = new SafeLocationFinder(this, configurationManager, searchExecutor);
        this.rtpService = new RtpService(this, configurationManager, cooldownManager, countdownManager, safeLocationFinder);
        RtpGui rtpGui = new RtpGui(configurationManager, rtpService);

        PluginCommand command = Objects.requireNonNull(getCommand("rtp"), "rtp command missing from plugin.yml");
        RtpCommand rtpCommand = new RtpCommand(configurationManager, rtpGui);
        command.setExecutor(rtpCommand);
        command.setTabCompleter(rtpCommand);

        getServer().getPluginManager().registerEvents(new GuiListener(rtpGui), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(countdownManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(rtpService, countdownManager), this);
        getServer().getPluginManager().registerEvents(new PlayerKickListener(rtpService, countdownManager), this);
    }

    @Override
    public void onDisable() {
        if (rtpService != null) {
            rtpService.shutdown();
        }
        if (countdownManager != null) {
            countdownManager.cancelAll(false);
        }
        if (searchExecutor != null) {
            searchExecutor.shutdownNow();
        }
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    private static final class SearchThreadFactory implements ThreadFactory {
        private int counter = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "WRTP-Search-" + counter++);
            thread.setDaemon(true);
            return thread;
        }
    }
}

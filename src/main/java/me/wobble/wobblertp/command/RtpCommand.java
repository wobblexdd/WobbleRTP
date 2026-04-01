package me.wobble.wobblertp.command;

import java.util.Collections;
import java.util.List;
import me.wobble.wobblertp.gui.RtpGui;
import me.wobble.wobblertp.manager.ConfigurationManager;
import me.wobble.wobblertp.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class RtpCommand implements CommandExecutor, TabCompleter {

    private final ConfigurationManager configurationManager;
    private final RtpGui rtpGui;

    public RtpCommand(ConfigurationManager configurationManager, RtpGui rtpGui) {
        this.configurationManager = configurationManager;
        this.rtpGui = rtpGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player player && !player.hasPermission("wobble.rtp.admin")) {
                player.sendMessage(ChatUtil.prefixed(configurationManager, "no-permission"));
                return true;
            }

            configurationManager.reload();
            sender.sendMessage(ChatUtil.prefixed(configurationManager, "reload-success"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.prefixed(configurationManager, "only-player"));
            return true;
        }

        if (!player.hasPermission("wobble.rtp.use")) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "no-permission"));
            return true;
        }

        if (args.length > 0) {
            player.sendMessage(ChatUtil.prefixed(configurationManager, "usage"));
            return true;
        }

        rtpGui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("wobble.rtp.admin")
                && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}

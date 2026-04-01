package me.wobble.wobblertp.util;

import java.util.ArrayList;
import java.util.List;
import me.wobble.wobblertp.manager.ConfigurationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class ChatUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ChatUtil() {
    }

    public static Component component(String input, TagResolver... placeholders) {
        return MINI_MESSAGE.deserialize(input == null ? "" : input, TagResolver.resolver(placeholders));
    }

    public static Component prefixed(ConfigurationManager configurationManager, String path, TagResolver... placeholders) {
        String content = configurationManager.getMessage("prefix") + configurationManager.getMessage(path);
        return component(content, placeholders);
    }

    public static List<Component> componentList(List<String> lines, TagResolver... placeholders) {
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(component(line, placeholders));
        }
        return components;
    }

    public static TagResolver placeholder(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    public static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        if (seconds > 0 || builder.isEmpty()) {
            builder.append(seconds).append("s");
        }
        return builder.toString().trim();
    }
}

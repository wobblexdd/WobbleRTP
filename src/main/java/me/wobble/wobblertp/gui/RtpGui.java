package me.wobble.wobblertp.gui;

import java.util.List;
import me.wobble.wobblertp.manager.ConfigurationManager;
import me.wobble.wobblertp.model.RtpWorldType;
import me.wobble.wobblertp.service.RtpService;
import me.wobble.wobblertp.util.ChatUtil;
import me.wobble.wobblertp.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RtpGui {

    private static final int SIZE = 27;
    private static final int SLOT_OVERWORLD = 11;
    private static final int SLOT_NETHER = 13;
    private static final int SLOT_END = 15;
    private static final int SLOT_CLOSE = 22;

    private final ConfigurationManager configurationManager;
    private final RtpService rtpService;

    public RtpGui(ConfigurationManager configurationManager, RtpService rtpService) {
        this.configurationManager = configurationManager;
        this.rtpService = rtpService;
    }

    public void open(Player player) {
        RtpMenuHolder holder = new RtpMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                ChatUtil.component(configurationManager.getMessage("gui-title")));
        holder.setInventory(inventory);

        fillBackground(inventory);
        inventory.setItem(SLOT_OVERWORLD, createButton(Material.GRASS_BLOCK, "gui.overworld.name", "gui.overworld.lore"));
        inventory.setItem(SLOT_NETHER, createButton(Material.NETHERRACK, "gui.nether.name", "gui.nether.lore"));
        inventory.setItem(SLOT_END, createButton(Material.END_STONE, "gui.the_end.name", "gui.the_end.lore"));
        inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, "gui.close.name", "gui.close.lore"));

        player.openInventory(inventory);
        SoundUtil.play(player, configurationManager.getSettings().sound("gui-open"));
    }

    public boolean isRtpMenu(Inventory inventory) {
        return inventory.getHolder() instanceof RtpMenuHolder;
    }

    public void handleClick(Player player, int rawSlot) {
        switch (rawSlot) {
            case SLOT_OVERWORLD -> {
                player.closeInventory();
                rtpService.beginTeleport(player, RtpWorldType.OVERWORLD);
            }
            case SLOT_NETHER -> {
                player.closeInventory();
                rtpService.beginTeleport(player, RtpWorldType.NETHER);
            }
            case SLOT_END -> {
                player.closeInventory();
                rtpService.beginTeleport(player, RtpWorldType.THE_END);
            }
            case SLOT_CLOSE -> player.closeInventory();
            default -> {
            }
        }
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.space());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        filler.setItemMeta(meta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack createButton(Material material, String namePath, String lorePath) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(ChatUtil.component(configurationManager.getMessage(namePath)));
        List<Component> lore = ChatUtil.componentList(configurationManager.getMessageList(lorePath));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }
}

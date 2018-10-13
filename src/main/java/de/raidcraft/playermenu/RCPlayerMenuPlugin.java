package de.raidcraft.playermenu;

import de.raidcraft.api.BasePlugin;
import de.raidcraft.api.action.ActionAPI;
import de.raidcraft.api.config.Comment;
import de.raidcraft.api.config.ConfigurationBase;
import de.raidcraft.api.config.Setting;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.api.items.CustomItemException;
import de.raidcraft.playermenu.actionapi.MenuTrigger;
import de.raidcraft.util.CustomItemUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin for testing various stuff and creating proof of concepts.
 */
@Getter
public class RCPlayerMenuPlugin extends BasePlugin {

    private LocalConfiguration config;
    private PlayerListener listener;

    @Override
    public void enable() {
        this.config = configure(new LocalConfiguration(this));
        if (getConfig().enabled) {
            listener = new PlayerListener(this);
            registerEvents(listener);
            ActionAPI.register(this).global()
                    .trigger(new MenuTrigger());
        }
    }

    @Override
    public void disable() {
        Bukkit.getScheduler().cancelTasks(this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!listener.getClearedCraftingFields().remove(player.getUniqueId())) {
                InventoryView view = player.getOpenInventory();
                if (PlayerListener.isPlayerCraftingInv(view)) {
                    view.getTopInventory().clear();
                }
            }
        }
    }

    public class LocalConfiguration extends ConfigurationBase<RCPlayerMenuPlugin> {

        @Comment("Interval in ticks for the task that refreshes the player menu inside the crafting field.")
        @Setting("task-interval")
        public long menuTaskInterval = 20L;
        @Setting("enabled")
        @Comment("You can disable the plugin by switching this on and off.")
        public boolean enabled = true;

        public LocalConfiguration(RCPlayerMenuPlugin plugin) {
            super(plugin, "config.yml");
        }
    }

}

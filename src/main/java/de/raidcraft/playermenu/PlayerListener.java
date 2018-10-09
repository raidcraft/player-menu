package de.raidcraft.playermenu;

import de.raidcraft.RaidCraft;
import de.raidcraft.hotbar.skills.RCSkillsHotbar;
import de.raidcraft.quests.QuestManager;
import de.raidcraft.quests.api.holder.QuestHolder;
import de.raidcraft.quests.api.quest.Quest;
import fr.zcraft.zlib.tools.items.ItemStackBuilder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class PlayerListener implements Listener {

    private static final int PLAYER_CRAFT_INV_SIZE = 5;
    private static final int CLEAR_INV_SLOT = 4;
    private static final int SETTINGS_SLOT = 3;
    private static final int SKILLS_MENU_SLOT = 2;
    private static final int QUEST_LOG_SLOT = 1;

    private final RCPlayerMenuPlugin plugin;
    private final Set<UUID> clearedCraftingFields = new HashSet<>();

    public PlayerListener(RCPlayerMenuPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (clearedCraftingFields.contains(player.getUniqueId())) {
                    continue;
                }

                InventoryView view = player.getOpenInventory();

                // If the open inventory is a player inventory
                // Update to the ring item
                // This will update even when it is closed, but
                // it is a small price to pay IMO
                if (isPlayerCraftingInv(view)) {
                    Inventory crafting = view.getTopInventory();
                    crafting.setItem(SKILLS_MENU_SLOT, getSkillMenu(player));
                    crafting.setItem(QUEST_LOG_SLOT, getQuestLog(player));
                    crafting.setItem(SETTINGS_SLOT, getSettingsItem(player));
                    crafting.setItem(CLEAR_INV_SLOT, getClearItem());
                }
            }
        }, 0, getPlugin().getConfig().menuTaskInterval);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (clearedCraftingFields.remove(event.getPlayer().getUniqueId())) {
            return;
        }

        InventoryView view = event.getView();

        // Remove the ring item in the matrix to prevent
        // players from duping them
        if (isPlayerCraftingInv(view)) {
            view.getTopInventory().clear();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (clearedCraftingFields.remove(event.getPlayer().getUniqueId())) {
            return;
        }

        InventoryView view = event.getPlayer().getOpenInventory();
        if (isPlayerCraftingInv(view)) {
            view.getTopInventory().clear();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (clearedCraftingFields.contains(event.getWhoClicked().getUniqueId())) {
            return;
        }

        InventoryView view = event.getView();

        // Don't allow players to remove anything from their
        // own crafting matrix
        // The view includes the player's entire inventory
        // as well, so check to make sure that the clicker
        // did not click on their own inventory
        if (isPlayerCraftingInv(view) &&
                event.getClickedInventory() != event.getWhoClicked().getInventory()) {
            if (event.getSlot() < 5) {
                switch (event.getSlot()) {
                    case CLEAR_INV_SLOT:
                        event.getClickedInventory().clear();
                        clearedCraftingFields.add(event.getWhoClicked().getUniqueId());
                        break;
                    case SKILLS_MENU_SLOT:
                        openSkillMenu((Player) event.getWhoClicked());
                        break;
                    case QUEST_LOG_SLOT:
                        openQuestLog((Player) event.getWhoClicked());
                        break;

                }
                event.setCancelled(true);
            }
        }
    }

    private static void openQuestLog(Player player) {
        RaidCraft.getComponent(QuestManager.class).openQuestLog(player);
    }

    private static void openSkillMenu(Player player) {
        RaidCraft.getComponent(RCSkillsHotbar.class).openSkillMenu(player);
    }

    private static boolean isPlayerCraftingInv(InventoryView view) {
        return view.getTopInventory().getType() == InventoryType.CRAFTING
                && view.getTopInventory().getSize() == PLAYER_CRAFT_INV_SIZE;
    }

    private static ItemStack getClearItem() {
        return new ItemStackBuilder(Material.BARRIER)
                .title(ChatColor.RED + "Crafting Feld räumen.")
                .lore(ChatColor.GRAY + "Linksklick: Entfernt das Menü aus dem Crafting Feld um Platz zum Craften zu machen.")
                .item();
    }

    private static ItemStack getSettingsItem(Player player) {
        return new ItemStackBuilder(Material.STRUCTURE_VOID).title("PLACEHOLDER").item();
    }

    private static ItemStack getSkillMenu(Player player) {
        return new ItemStackBuilder(Material.GOLD_SWORD)
                .title(ChatColor.GOLD + "Skill Menü")
                .lore(ChatColor.GRAY + "Linksklick: Öffnet das Skill Menü.")
                .item();
    }

    private static ItemStack getQuestLog(Player player) {
        QuestHolder questHolder = RaidCraft.getComponent(QuestManager.class).getQuestHolder(player);
        if (questHolder == null) {
            return new ItemStackBuilder(Material.BOOK)
                    .title(ChatColor.RED + "Quest Log nicht verfügbar.")
                    .item();
        }
        List<Quest> activeQuests = questHolder.getActiveQuests();
        return new ItemStackBuilder(Material.BOOK_AND_QUILL)
                .title(ChatColor.GOLD + "Quest Log")
                .lore(ChatColor.AQUA + "" + activeQuests.size() + ChatColor.WHITE + " aktive Quests.",
                        ChatColor.GRAY + "Linksklick: Öffnet das Quest Log.").item();
    }
}

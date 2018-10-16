package de.raidcraft.playermenu;

import de.raidcraft.RaidCraft;
import de.raidcraft.hotbar.skills.RCSkillsHotbar;
import de.raidcraft.quests.QuestManager;
import de.raidcraft.quests.api.holder.QuestHolder;
import de.raidcraft.quests.api.quest.Quest;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.hero.Hero;
import fr.zcraft.zlib.tools.items.ItemStackBuilder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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
    private static final int QUEST_INVENTORY_SLOT = 2;
    private static final int SKILLS_MENU_SLOT = 3;
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
                    crafting.setItem(QUEST_INVENTORY_SLOT, getQuestInventory(player));
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (clearedCraftingFields.remove(event.getEntity().getUniqueId())) {
            return;
        }

        InventoryView view = event.getEntity().getOpenInventory();
        if (isPlayerCraftingInv(view)) {
            view.getTopInventory().clear();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (clearedCraftingFields.contains(event.getWhoClicked().getUniqueId())) {
            return;
        }
        if (event.getClickedInventory() == null) return;

        ItemStack clickedItem = event.getClickedInventory().getItem(event.getSlot());
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

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
                    case QUEST_INVENTORY_SLOT:
                        openQuestInventory((Player) event.getWhoClicked());
                        break;

                }
                event.setCancelled(true);
            }
        }
    }

    private static void openQuestLog(Player player) {
        OpenMenuEvent event = new OpenMenuEvent(player, OpenMenuEvent.Type.QUEST);
        RaidCraft.callEvent(event);
        if (event.isCancelled()) return;
        RaidCraft.getComponent(QuestManager.class).openQuestLog(player);
    }

    private static void openQuestInventory(Player player) {
        QuestHolder questHolder = RaidCraft.getComponent(QuestManager.class).getQuestHolder(player);
        if (questHolder == null) return;
        OpenMenuEvent event = new OpenMenuEvent(player, OpenMenuEvent.Type.QUEST_INVENTORY);
        RaidCraft.callEvent(event);
        if (event.isCancelled()) return;
        questHolder.getQuestInventory().open();
    }

    private static void openSkillMenu(Player player) {
        OpenMenuEvent event = new OpenMenuEvent(player, OpenMenuEvent.Type.SKILLS);
        RaidCraft.callEvent(event);
        if (event.isCancelled()) return;
        RaidCraft.getComponent(RCSkillsHotbar.class).openSkillMenu(player);
    }

    public static boolean isPlayerCraftingInv(InventoryView view) {
        return view.getTopInventory().getType() == InventoryType.CRAFTING
                && view.getTopInventory().getSize() == PLAYER_CRAFT_INV_SIZE;
    }

    private static ItemStack getClearItem() {
        return new ItemStackBuilder(Material.BARRIER)
                .title(ChatColor.RED + "Crafting Feld räumen.")
                .lore(ChatColor.GRAY + "Klick: Entfernt das Menü aus dem Crafting Feld um Platz zum Craften zu machen.")
                .item();
    }

    private static ItemStack getQuestInventory(Player player) {

        QuestHolder questHolder = RaidCraft.getComponent(QuestManager.class).getQuestHolder(player);
        if (questHolder == null) return new ItemStack(Material.AIR);

        return new ItemStackBuilder(Material.ENDER_CHEST)
                .title(ChatColor.GOLD + "Quest Inventar")
                .lore(ChatColor.AQUA + "" + questHolder.getQuestInventory().count() + ChatColor.WHITE + " Items im Quest Inventar.",
                        ChatColor.GRAY + "Klick: Öffnet das Quest Log.").item();
    }

    private ItemStack getSkillMenu(Player player) {
        Hero hero = RaidCraft.getComponent(CharacterManager.class).getHero(player);
        if (hero == null) return new ItemStack(Material.AIR);

        if (hero.getVirtualProfession().equals(hero.getHighestRankedProfession())) return new ItemStack(Material.AIR);

        return new ItemStackBuilder(Material.GOLD_SWORD)
                .title(ChatColor.GOLD + "Skill Menü")
                .lore(ChatColor.GRAY + "Klick: Öffnet das Skill Menü.")
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
                        ChatColor.GRAY + "Klick: Öffnet das Quest Log.").item();
    }
}

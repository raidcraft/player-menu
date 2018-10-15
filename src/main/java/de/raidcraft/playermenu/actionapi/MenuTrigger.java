package de.raidcraft.playermenu.actionapi;

import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.playermenu.OpenMenuEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MenuTrigger extends Trigger implements Listener {

    public MenuTrigger() {
        super("playermenu", "open");
    }

    @EventHandler
    @Information(
            value = "playermenu.open",
            desc = "Triggers when the player opens his player menu",
            conf = {
                    "menu: <skills,quest,quest_inventory>"
            }
    )
    public void onMenuOpen(OpenMenuEvent event) {

        informListeners("open", event.getPlayer(), config -> {
            if (!config.isSet("menu")) return true;
            return config.getString("menu").equalsIgnoreCase(event.getMenu().name());
        });
    }
}

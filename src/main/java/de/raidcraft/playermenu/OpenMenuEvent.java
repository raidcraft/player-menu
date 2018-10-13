package de.raidcraft.playermenu;

import lombok.Data;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Data
public class OpenMenuEvent extends Event implements Cancellable {

    public enum Type {
        QUEST,
        SKILLS,
        QUEST_INVENTORY
    }

    private final Player player;
    private final Type menu;
    private boolean cancelled;

    //<-- Handler -->//
    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}

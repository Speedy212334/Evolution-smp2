package me.ph4ntomgg.evolutionsmp.fx;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/**
 * Cross-plugin visual cue emitted by EvolutionSMP after a gameplay action succeeds.
 * EvolutionFX may render the cue when installed; gameplay never depends on it.
 */
public final class EvolutionVisualEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String animationId;
    private final int durationSeconds;

    public EvolutionVisualEvent(Player player, String animationId, int durationSeconds) {
        this.player = Objects.requireNonNull(player, "player");
        this.animationId = Objects.requireNonNull(animationId, "animationId");
        this.durationSeconds = Math.max(1, Math.min(30, durationSeconds));
    }

    public Player getPlayer() { return player; }
    public String getAnimationId() { return animationId; }
    public int getDurationSeconds() { return durationSeconds; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

package me.ph4ntomgg.evolutionsmp.fx;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Bridges successful core gameplay actions to optional configured visual presets. */
public final class EvolutionFXListener implements Listener {
    private final CustomAnimationManager animations;

    public EvolutionFXListener(CustomAnimationManager animations) {
        this.animations = animations;
    }

    @EventHandler
    public void onEvolutionVisual(EvolutionVisualEvent event) {
        if (event.getPlayer().isOnline()) {
            animations.play(event.getPlayer(), event.getAnimationId(), event.getDurationSeconds());
        }
    }
}

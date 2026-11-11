package me.ph4ntomgg.evolutionsmp.domain;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PlayerProgressTest {
    @Test
    void pathChangerPreservesTierAndResetsPathXp() {
        PlayerProgress current = PlayerProgress.unassigned(UUID.randomUUID())
                .withPath(EvolutionPath.HUNT)
                .withTier(EvolutionTier.T3, 275);
        PlayerProgress changed = current.withPathPreservingTier(EvolutionPath.ENDER);
        assertEquals(EvolutionPath.ENDER, changed.path());
        assertEquals(EvolutionTier.T3, changed.tier());
        assertEquals(0, changed.experience());
        assertFalse(changed.apex());
    }

    @Test
    void pathChangerPreservesApexState() {
        PlayerProgress current = PlayerProgress.unassigned(UUID.randomUUID())
                .withPath(EvolutionPath.TRADE)
                .withTier(EvolutionTier.APEX, 0);
        PlayerProgress changed = current.withPathPreservingTier(EvolutionPath.CALAMITY);
        assertEquals(EvolutionTier.APEX, changed.tier());
        assertTrue(changed.apex());
    }
}

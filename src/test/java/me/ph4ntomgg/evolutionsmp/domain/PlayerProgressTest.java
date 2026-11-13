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

    @Test
    void normalPathAssignmentResetsApexAndTier() {
        PlayerProgress current = PlayerProgress.unassigned(UUID.randomUUID())
                .withPath(EvolutionPath.TRADE)
                .withTier(EvolutionTier.APEX, 0);
        PlayerProgress reassigned = current.withPath(EvolutionPath.HUNT);
        assertEquals(EvolutionTier.T1, reassigned.tier());
        assertEquals(0, reassigned.experience());
        assertFalse(reassigned.apex());
    }

    @Test
    void rejectsInconsistentApexState() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerProgress(id, EvolutionPath.HUNT, EvolutionTier.APEX, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerProgress(id, EvolutionPath.HUNT, EvolutionTier.T4, 0, true));
    }

    @Test
    void rejectsExperienceAtTierFour() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerProgress(UUID.randomUUID(), EvolutionPath.HUNT, EvolutionTier.T4, 1, false));
    }

    @Test
    void rejectsUnassignedProgressOutsideTierOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerProgress(UUID.randomUUID(), null, EvolutionTier.T2, 0, false));
    }
}

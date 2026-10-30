package me.ph4ntomgg.evolutionsmp.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvolutionPathTest {
    @Test
    void registersExactlySevenPaths() {
        assertEquals(7, EvolutionPath.values().length);
    }

    @Test
    void resolvesPathIdsAndDisplayNamesWithoutCaseSensitivity() {
        assertEquals(EvolutionPath.CALAMITY, EvolutionPath.fromId("calamity").orElseThrow());
        assertEquals(EvolutionPath.ENDER, EvolutionPath.fromId("Ender").orElseThrow());
        assertTrue(EvolutionPath.fromId("not-a-path").isEmpty());
        assertTrue(EvolutionPath.fromId(null).isEmpty());
    }
}

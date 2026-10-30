package me.ph4ntomgg.evolutionsmp.domain;

import java.util.Arrays;
import java.util.Optional;

public enum EvolutionPath {
    HUNT("Hunt"),
    ALCHEMY("Alchemy"),
    ENCHANTMENT("Enchantment"),
    PROSPECTOR("Prospector"),
    ENDER("Ender"),
    TRADE("Trade"),
    CALAMITY("Calamity");

    private final String displayName;

    EvolutionPath(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<EvolutionPath> fromId(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Arrays.stream(values())
                .filter(path -> path.name().equalsIgnoreCase(id)
                        || path.displayName.equalsIgnoreCase(id))
                .findFirst();
    }
}

package me.ph4ntomgg.evolutionsmp.guide;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;

public final class EvolutionGuide {
    private static final Map<String, String[]> TOPICS = Map.of(
            "paths", new String[]{
                    "There are seven paths: Hunt, Alchemy, Enchantment, Prospector, Ender, Trade and Calamity.",
                    "Each path has its own XP sources and abilities. Your path is assigned by an administrator."
            },
            "tiers", new String[]{
                    "Normal progression is T1 → T2 → T3 → T4. Apex is a separate secret tier.",
                    "XP thresholds currently are 1,000 to T2, 3,500 additional XP to T3, and 9,000 additional XP to T4.",
                    "Reaching T4 does not automatically grant Apex."
            },
            "death", new String[]{
                    "Dying at T2, T3 or T4 drops a tier-specific Evolution Orb and resets the player to T1.",
                    "Apex players do not drop Evolution Orbs and do not regress through this death rule."
            },
            "orbs", new String[]{
                    "T2, T3 and T4 Evolution Orbs have distinct items and crafting recipes.",
                    "T2 uses iron, diamond and emerald blocks. T3 uses netherite ingots, totems and a beacon.",
                    "T4 uses a Nether Star, totems and Evolution Shards. It does not require a previous tier orb."
            },
            "abilities", new String[]{
                    "Trade Apex can activate /evo gambit: three minutes of invulnerability, then all armor in the player's inventory is destroyed and armor is locked for 20 minutes.",
                    "Calamity Apex can activate /evo heart: Strength IV, Speed III and Resistance II for 60 seconds, with a heart symbol above the player for three minutes.",
                    "Both abilities share a 30-minute cooldown in this first implementation."
            },
            "xp", new String[]{
                    "Hunt gains XP from hostile mob and player kills.",
                    "Alchemy gains XP from completed brewing batches and potion consumption.",
                    "Prospector gains XP from mining ores. Enchantment gains XP from enchanting.",
                    "Ender gains XP from Ender Pearl travel and entering the End. Trade gains XP from villager trades.",
                    "Calamity gains XP from boss kills."
            }
    );

    private EvolutionGuide() {}

    public static void send(CommandSender sender, String topic) {
        if (topic == null || topic.isBlank()) {
            sender.sendMessage(ChatColor.GOLD + "Evolution Guide: paths, tiers, death, orbs, abilities, xp");
            sender.sendMessage(ChatColor.GRAY + "Use /evo guide <topic> for a short explanation.");
            return;
        }
        String key = topic.toLowerCase(Locale.ROOT);
        String[] lines = TOPICS.get(key);
        if (lines == null) {
            sender.sendMessage(ChatColor.RED + "I don't have a guide entry for that topic yet.");
            send(sender, null);
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Evolution Guide » " + key.toUpperCase(Locale.ROOT));
        for (String line : lines) sender.sendMessage(ChatColor.GRAY + "• " + line);
    }
}

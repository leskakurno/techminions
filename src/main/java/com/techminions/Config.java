package com.techminions;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BASE_TICKS = BUILDER
            .comment("Ticks per mined block at minion level I (100 = 5 seconds). Higher levels and Speed Upgrades make it faster.")
            .defineInRange("baseTicksPerBlock", 100, 20, 6000);

    public static final ModConfigSpec.IntValue MIN_TICKS = BUILDER
            .comment("Absolute minimum ticks per mined block (prevents lag from over-upgraded minions).")
            .defineInRange("minTicksPerBlock", 10, 5, 6000);

    public static final ModConfigSpec.IntValue PLATFORM_Y_OFFSET = BUILDER
            .comment("Vertical offset of the 5x5 platform relative to the minion's feet block. 0 = same level as the minion, -1 = one block below (only placed into empty air).")
            .defineInRange("platformYOffset", 0, -1, 2);

    public static final ModConfigSpec.DoubleValue UPGRADE_COST_MULTIPLIER = BUILDER
            .comment("Multiplier for minion upgrade costs (1.0 = default). Set the same value on server and client.")
            .defineInRange("upgradeCostMultiplier", 1.0, 0.1, 20.0);

    public static final ModConfigSpec.IntValue HUNT_MOB_SPAWN_SEC = BUILDER
            .comment("Hunter minions: seconds for a normal mob to 'spawn' (from the hunt egg) before it can be killed.")
            .defineInRange("huntMobSpawnSeconds", 15, 1, 100000);

    public static final ModConfigSpec.IntValue HUNT_MOB_KILL_SEC = BUILDER
            .comment("Hunter minions: seconds it takes to kill a normal mob.")
            .defineInRange("huntMobKillSeconds", 15, 1, 100000);

    public static final ModConfigSpec.IntValue HUNT_BOSS_SPAWN_MIN = BUILDER
            .comment("Hunter minions: minutes for a boss to 'spawn' (from the hunt egg).")
            .defineInRange("huntBossSpawnMinutes", 45, 1, 100000);

    public static final ModConfigSpec.IntValue HUNT_BOSS_KILL_MIN = BUILDER
            .comment("Hunter minions: minutes it takes to kill a boss.")
            .defineInRange("huntBossKillMinutes", 15, 1, 100000);

    public static final ModConfigSpec.BooleanValue HUNT_SCALE = BUILDER
            .comment("If true, minion level and Speed Upgrades shorten the hunt times above (like mining). Default false = exact times.")
            .define("huntScaleWithUpgrades", false);

    public static final ModConfigSpec.DoubleValue EGG_CHANCE_PER_LEVEL = BUILDER
            .comment("Egg Collector enchantment: chance (0.001 = 0.1%) per level that a mob killed by a player drops its Hunt Egg.")
            .defineInRange("eggChancePerLevel", 0.001, 0.0, 0.2);

    public static final ModConfigSpec.DoubleValue EGG_LOOTING_BONUS = BUILDER
            .comment("Each Looting level on the same weapon raises the egg chance by this fraction (0.5 = +50% per level).")
            .defineInRange("eggLootingBonusPerLevel", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue EGG_BOSS_MULT = BUILDER
            .comment("Multiplier of the egg chance for bosses (boss kills are rare, so they get a higher chance).")
            .defineInRange("eggBossChanceMultiplier", 5.0, 0.0, 100.0);

    private static final String DEFAULT_BOSSES = String.join(",",
            "twilightforest:naga", "twilightforest:hydra", "twilightforest:lich", "twilightforest:minoshroom",
            "twilightforest:knight_phantom", "twilightforest:ur_ghast", "twilightforest:alpha_yeti",
            "twilightforest:snow_queen",
            "mowziesmobs:ferrous_wroughtnaut", "mowziesmobs:frostmaw", "mowziesmobs:umvuthi", "mowziesmobs:sculptor",
            "cataclysm:ender_guardian", "cataclysm:netherite_monstrosity", "cataclysm:ignis", "cataclysm:the_harbinger",
            "cataclysm:the_leviathan", "cataclysm:scylla", "cataclysm:ancient_remnant", "cataclysm:maledictus",
            "alexscaves:luxtructosaurus", "alexscaves:tremorzilla", "alexsmobs:void_worm",
            "graveyard:lich", "graveyard:nightmare");

    public static final ModConfigSpec.ConfigValue<String> BOSS_ENTITIES = BUILDER
            .comment("Extra entity ids (comma-separated) that count as bosses for hunter minions and Hunt Eggs, "
                    + "in addition to everything in the c:bosses entity tag. Unknown ids are simply ignored.")
            .define("bossEntities", DEFAULT_BOSSES);

    private static String bossRaw;
    private static java.util.Set<String> bossSet = java.util.Set.of();

    /** Ids существ-боссов из конфига (разбор кэшируется). */
    public static synchronized java.util.Set<String> bossEntityIds() {
        String raw = BOSS_ENTITIES.get();
        if (!raw.equals(bossRaw)) {
            java.util.Set<String> set = new java.util.HashSet<>();
            for (String part : raw.split(",")) {
                String id = part.trim();
                if (!id.isEmpty()) {
                    set.add(id);
                }
            }
            bossSet = set;
            bossRaw = raw;
        }
        return bossSet;
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}

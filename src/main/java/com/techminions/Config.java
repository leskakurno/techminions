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

    public static final ModConfigSpec SPEC = BUILDER.build();
}

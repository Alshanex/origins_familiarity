package net.alshanex.origins_familiarity;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class FOConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue STUN_SECONDS = BUILDER
            .comment("Seconds the bound familiar stays stunned instead of dying.")
            .defineInRange("stunSecondsPerSummonedFamiliar", 5, 1, 600);

    public static final ModConfigSpec.DoubleValue SPELLBOOK_BONUS_FRACTION = BUILDER
            .comment("Fraction of the owner's bonus magic attributes given to the bound familiar.")
            .defineInRange("spellbookBonusFraction", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.BooleanValue SHARE_ORIGIN_POWERS = BUILDER
            .comment("Copy the owner's passive origin powers onto the bound familiar.")
            .define("shareOriginPowers", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DENIED_POWER_TYPES = BUILDER
            .comment("Power types that are never copied onto the bound familiar (on top of every power that uses a keybind).")
            .defineListAllowEmpty("deniedPowerTypes", List.of(
                    "apoli:action_on_key_press",
                    "apoli:action_on_key_sequence",
                    "apoli:action_on_scroll_wheel",
                    "apoli:action_on_mouse_movement",
                    "apoli:prevent_key_press",
                    "apoli:toggle",
                    "apoli:inventory",
                    "apoli:keep_inventory",
                    "apoli:creative_flight",
                    "apoli:elytra_flight",
                    "apoli:fire_projectile",
                    "apoli:modify_camera",
                    "apoli:modify_fov",
                    "apoli:modify_fog",
                    "apoli:modify_food",
                    "apoli:modify_exhaustion",
                    "apoli:edible_item",
                    "apoli:modify_player_spawn"
            ), () -> "", o -> o instanceof String);

    public static final ModConfigSpec.BooleanValue SUMMON_ON_CREATE = BUILDER
            .comment("Summon the bound familiar right after the player chooses its form.")
            .define("summonWhenCreated", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FOConfig() {
    }
}

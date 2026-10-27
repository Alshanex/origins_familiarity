package net.alshanex.origins_familiarity.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record LearnedSpell(ResourceLocation id, int level, boolean enabled) {

    public LearnedSpell withEnabled(boolean value) {
        return new LearnedSpell(id, level, value);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        tag.putInt("level", level);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Nullable
    public static LearnedSpell fromTag(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        if (id == null) return null;
        return new LearnedSpell(id, Math.max(1, tag.getInt("level")), !tag.contains("enabled") || tag.getBoolean("enabled"));
    }
}

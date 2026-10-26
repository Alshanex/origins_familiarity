package net.alshanex.origins_familiarity.bound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** Which form the bound familiar has (NONE until chosen) and its UUID. */
public final class BoundPlayerData {
    public static final Codec<BoundPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("form", "none").forGetter(data -> data.form.getSerializedName()),
            UUIDUtil.CODEC.optionalFieldOf("bound_id").forGetter(data -> Optional.ofNullable(data.boundId))
    ).apply(instance, (form, id) -> new BoundPlayerData(BoundForm.byName(form), id.orElse(null))));

    private BoundForm form = BoundForm.NONE;
    @Nullable private UUID boundId;

    public BoundPlayerData() {}

    private BoundPlayerData(BoundForm form, @Nullable UUID boundId) {
        this.form = form;
        this.boundId = boundId;
    }

    public BoundForm form() { return form; }
    public void setForm(BoundForm form) { this.form = form; }

    @Nullable public UUID boundId() { return boundId; }
    public void setBoundId(@Nullable UUID boundId) { this.boundId = boundId; }
}

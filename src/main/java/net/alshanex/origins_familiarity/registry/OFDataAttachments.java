package net.alshanex.origins_familiarity.registry;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.bound.BoundPlayerData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class OFDataAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, OriginsFamiliarity.MODID);

    public static final Supplier<AttachmentType<BoundPlayerData>> BOUND_DATA = ATTACHMENTS.register("bound_data",
            () -> AttachmentType.builder(BoundPlayerData::new)
                    .serialize(BoundPlayerData.CODEC)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}

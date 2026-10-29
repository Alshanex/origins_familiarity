package net.alshanex.origins_familiarity.registry;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.sets.FamiliarSetItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OFItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OriginsFamiliarity.MODID);

    public static final DeferredItem<FamiliarSetItem> FAMILIAR_SET = ITEMS.register("familiar_set",
            () -> new FamiliarSetItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}

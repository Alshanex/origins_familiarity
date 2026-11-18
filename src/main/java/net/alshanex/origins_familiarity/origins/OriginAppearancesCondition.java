package net.alshanex.origins_familiarity.origins;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.alshanex.origins_familiarity.OFConfig;

public class OriginAppearancesCondition implements ConditionType<EntityCtx, OriginAppearancesCondition.Cfg>{
    public record Cfg() {}

    @Override
    public MapCodec<Cfg> codec() {
        return MapCodec.unit(new Cfg());
    }

    @Override
    public boolean test(Cfg config, EntityCtx ctx) {
        return OFConfig.originAppearances();
    }
}

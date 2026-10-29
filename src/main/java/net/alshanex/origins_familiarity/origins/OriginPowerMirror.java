package net.alshanex.origins_familiarity.origins;

import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.data.FunctionalKey;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import dev.overgrown.origins.origin.OriginManager;
import net.alshanex.origins_familiarity.OFConfig;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copies the owner's passive origin powers onto the bound familiar, under its own power source so they are added and removed as the owner's origins change.
 */
public final class OriginPowerMirror {
    public static final ResourceLocation SOURCE = OriginsFamiliarity.id("owner_origin");
    public static final String TAG_ALWAYS = "origins_familiarity:always_share";
    public static final String TAG_NEVER = "origins_familiarity:never_share";

    private static final Map<ResourceLocation, Boolean> PASSIVE_CACHE = new ConcurrentHashMap<>();
    private static volatile int cachedGeneration = Integer.MIN_VALUE;
    private static volatile List<? extends String> cachedDenyList = List.of();

    private static volatile int cachedNeverShareGeneration = -1;

    public static void sync(LivingEntity familiar, ServerPlayer owner) {
        boolean enabled = OFConfig.SHARE_ORIGIN_POWERS.get();
        PowerContainer familiarPowers = enabled
                ? PowerContainerAttachment.getOrCreate(familiar)
                : PowerContainerAttachment.get(familiar);
        if (familiarPowers == null) return;

        Set<ResourceLocation> desired = enabled ? passiveOriginPowers(owner) : Set.of();
        Set<ResourceLocation> current = new HashSet<>();
        for (ResourceLocation power : familiarPowers.allPowers()) {
            if (familiarPowers.sourcesOf(power).contains(SOURCE)) {
                current.add(power);
            }
        }
        if (current.equals(desired)) return;

        for (ResourceLocation power : current) {
            if (!desired.contains(power)) {
                try {
                    familiarPowers.removePower(power, SOURCE);
                } catch (Exception e) {
                    OriginsFamiliarity.LOGGER.error("Failed to remove power {} from bound familiar", power, e);
                }
            }
        }
        for (ResourceLocation power : desired) {
            if (!current.contains(power)) {
                try {
                    familiarPowers.addPower(power, SOURCE);
                } catch (Exception e) {
                    OriginsFamiliarity.LOGGER.error("Failed to give power {} to bound familiar", power, e);
                }
            }
        }
    }

    private static Set<ResourceLocation> passiveOriginPowers(ServerPlayer owner) {
        PowerContainer ownerPowers = PowerContainer.of(owner);
        if (ownerPowers == null) return Set.of();

        Set<ResourceLocation> result = new HashSet<>();
        for (ResourceLocation power : ownerPowers.allPowers()) {
            if (isFromOrigin(ownerPowers.sourcesOf(power)) && isPassive(power)) {
                result.add(power);
            }
        }
        return result;
    }

    private static boolean isFromOrigin(Set<ResourceLocation> sources) {
        for (ResourceLocation source : sources) {
            if (OriginManager.layerOfSource(source) != null) return true;
        }
        return false;
    }

    private static boolean isPassive(ResourceLocation powerId) {
        int generation = ApoliPowers.generation();
        int neverShareGeneration = NeverSharePowers.INSTANCE.generation();
        List<? extends String> deny = OFConfig.DENIED_POWER_TYPES.get();
        if (generation != cachedGeneration || neverShareGeneration != cachedNeverShareGeneration || deny != cachedDenyList) {
            PASSIVE_CACHE.clear();
            cachedGeneration = generation;
            cachedNeverShareGeneration = neverShareGeneration;
            cachedDenyList = deny;
        }
        return PASSIVE_CACHE.computeIfAbsent(powerId, id -> computePassive(id, 0));
    }

    private static boolean computePassive(ResourceLocation powerId, int depth) {
        Power power = ApoliPowers.get(powerId);
        if (power == null || depth > 4) return false;
        if (power.hasTag(TAG_NEVER)) return false;
        if (power.hasTag(TAG_ALWAYS)) return true;
        if (NeverSharePowers.INSTANCE.isNeverShared(powerId, power)) return false;

        for (String denied : cachedDenyList) {
            ResourceLocation deniedId = ResourceLocation.tryParse(denied);
            if (deniedId != null && PowerTypeRegistry.resolveId(deniedId).equals(power.typeId())) {
                return false;
            }
        }
        if (usesKey(power.config(), 0)) return false;

        if (power.config() instanceof MultiplePower.Cfg multiple) {
            for (ResourceLocation sub : multiple.subPowerIds()) {
                if (!computePassive(sub, depth + 1)) return false;
            }
        }
        return true;
    }

    /** Any keybind anywhere in the power's config (record components, optionals, lists) means it's an active power. */
    private static boolean usesKey(Object value, int depth) {
        if (value == null || depth > 4) return false;
        if (value instanceof Key || value instanceof FunctionalKey) return true;
        if (value instanceof Optional<?> optional) return optional.isPresent() && usesKey(optional.get(), depth + 1);
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                if (usesKey(element, depth + 1)) return true;
            }
            return false;
        }
        Class<?> type = value.getClass();
        if (!type.isRecord()) return false;
        for (RecordComponent component : type.getRecordComponents()) {
            try {
                var accessor = component.getAccessor();
                accessor.setAccessible(true);
                if (usesKey(accessor.invoke(value), depth + 1)) return true;
            } catch (Exception ignored) {
                // Unreadable component: assume it isn't a key
            }
        }
        return false;
    }
}

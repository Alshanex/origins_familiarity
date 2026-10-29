package net.alshanex.origins_familiarity.origins;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data-driven "never share with the familiar" list: data/<namespace>/origins_familiarity/never_share/*.json.
 */
public final class NeverSharePowers extends SimpleJsonResourceReloadListener {
    public static final NeverSharePowers INSTANCE = new NeverSharePowers();

    private volatile Set<ResourceLocation> powerTypes = Set.of();
    private volatile Set<ResourceLocation> powers = Set.of();
    private final AtomicInteger generation = new AtomicInteger();

    private NeverSharePowers() {
        super(new Gson(), "origins_familiarity/never_share");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Set<ResourceLocation> types = new HashSet<>();
        Set<ResourceLocation> ids = new HashSet<>();
        files.forEach((file, json) -> {
            try {
                JsonObject object = GsonHelper.convertToJsonObject(json, "never_share");
                read(object, "power_types", types, file);
                read(object, "powers", ids, file);
            } catch (Exception e) {
                OriginsFamiliarity.LOGGER.error("Couldn't read never_share list {}", file, e);
            }
        });

        Set<ResourceLocation> resolved = new HashSet<>();
        for (ResourceLocation type : types) resolved.add(PowerTypeRegistry.resolveId(type));
        powerTypes = Set.copyOf(resolved);
        powers = Set.copyOf(ids);
        generation.incrementAndGet();
    }

    private static void read(JsonObject object, String key, Set<ResourceLocation> out, ResourceLocation file) {
        if (!object.has(key)) return;
        for (JsonElement element : GsonHelper.getAsJsonArray(object, key)) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            if (id == null) {
                OriginsFamiliarity.LOGGER.warn("Invalid id '{}' in never_share list {}", element, file);
            } else {
                out.add(id);
            }
        }
    }

    public boolean isNeverShared(ResourceLocation powerId, Power power) {
        return powers.contains(powerId) || powerTypes.contains(power.typeId());
    }

    public int generation() {
        return generation.get();
    }
}
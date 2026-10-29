package net.alshanex.origins_familiarity.sets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which set's owner bonuses each player has right now.
 */
public final class OwnerSetState {
    private static final Map<UUID, ResourceLocation> ACTIVE = new ConcurrentHashMap<>();

    @Nullable
    public static ResourceLocation get(Player player) {
        return ACTIVE.get(player.getUUID());
    }

    public static void set(UUID playerId, @Nullable ResourceLocation setId) {
        if (setId == null) {
            ACTIVE.remove(playerId);
        } else {
            ACTIVE.put(playerId, setId);
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
    }
}

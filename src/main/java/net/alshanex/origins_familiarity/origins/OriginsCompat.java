package net.alshanex.origins_familiarity.origins;

import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.origin.OriginManager;
import net.minecraft.server.level.ServerPlayer;

public final class OriginsCompat {
    /** True once the player has an origin on every layer that offers one and the choose screen is done. */
    public static boolean hasFinishedChoosing(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null || state.isSelectingOrigin()) {
            return false;
        }
        return OriginManager.hasChosenAllLayers(player, state);
    }
}
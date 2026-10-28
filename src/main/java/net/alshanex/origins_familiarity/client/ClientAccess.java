package net.alshanex.origins_familiarity.client;

import net.alshanex.origins_familiarity.client.screen.BoundSpellsScreen;
import net.alshanex.origins_familiarity.network.BoundSpellsPayload;
import net.minecraft.client.Minecraft;

public final class ClientAccess {

    public static void showBoundSpells(BoundSpellsPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BoundSpellsScreen screen) {
            screen.update(payload.entries());
        } else if (mc.screen == null) {
            mc.setScreen(new BoundSpellsScreen(payload.entries()));
        }
    }
}

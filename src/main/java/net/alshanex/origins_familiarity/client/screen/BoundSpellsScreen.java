package net.alshanex.origins_familiarity.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.entity.IBoundFamiliar;
import net.alshanex.origins_familiarity.network.BoundSpellsPayload;
import net.alshanex.origins_familiarity.network.ToggleBoundSpellPayload;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Keybind screen.
 */
public class BoundSpellsScreen extends Screen {
    private static final int COLUMNS = 6;
    private static final int ICON = 32;
    private static final int BORDER = 2;
    private static final int CELL = ICON + BORDER * 2;
    private static final int GAP = 14;
    private static final int PADDING = 16;
    private static final int TITLE_SPACE = 14;

    private static final int ENABLED_COLOR = 0xFF3FB950;
    private static final int DISABLED_COLOR = 0xFFD03A3A;
    private static final int CELL_BACKGROUND = 0xFF1B1B24;
    private static final int HOVER_TINT = 0x30FFFFFF;
    private static final int PANEL_COLOR = 0xCC101018;
    private static final int PANEL_OUTLINE = 0xFF6A4FB3;

    private static final int SET_COLOR = 0xFFE0B040;

    /** Desaturated copies of spell icons. */
    private static final Map<ResourceLocation, ResourceLocation> GREYSCALE_ICONS = new HashMap<>();

    private List<BoundSpellsPayload.Entry> entries;
    private int scrollRow;
    private int visibleRows;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int gridLeft;
    private int gridTop;

    public BoundSpellsScreen(List<BoundSpellsPayload.Entry> entries) {
        super(Component.translatable("ui.origins_familiarity.spells.title"));
        this.entries = new ArrayList<>(entries);
    }

    public void update(List<BoundSpellsPayload.Entry> newEntries) {
        this.entries = new ArrayList<>(newEntries);
        layout();
    }

    @Override
    protected void init() {
        super.init();
        layout();
    }

    private void layout() {
        int rowsNeeded = Math.max(1, Mth.ceil(entries.size() / (float) COLUMNS));
        int maxRows = Math.max(1, (height - 40 - TITLE_SPACE - PADDING * 2 + GAP) / (CELL + GAP));
        visibleRows = Math.min(rowsNeeded, maxRows);

        panelWidth = PADDING * 2 + COLUMNS * CELL + (COLUMNS - 1) * GAP;
        panelHeight = PADDING * 2 + TITLE_SPACE + visibleRows * CELL + (visibleRows - 1) * GAP;
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        gridLeft = panelLeft + PADDING;
        gridTop = panelTop + PADDING + TITLE_SPACE;
        scrollRow = Mth.clamp(scrollRow, 0, maxScrollRow());
    }

    private int maxScrollRow() {
        int rowsNeeded = Mth.ceil(entries.size() / (float) COLUMNS);
        return Math.max(0, rowsNeeded - visibleRows);
    }

    // Rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL_COLOR);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, PANEL_OUTLINE);
        graphics.drawCenteredString(font, title, width / 2, panelTop + PADDING / 2 + 2, 0xFFFFFF);

        if (entries.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("ui.origins_familiarity.spells.empty"),
                    gridLeft, gridTop + 4, panelWidth - PADDING * 2, 0xAAAAAA);
            return;
        }

        int hovered = indexAt(mouseX, mouseY);
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollRow + row) * COLUMNS + col;
                if (index >= entries.size()) break;
                renderSpell(graphics, entries.get(index), cellX(col), cellY(row), index == hovered);
            }
        }

        if (maxScrollRow() > 0) {
            String pages = (scrollRow + 1) + "/" + (maxScrollRow() + 1);
            graphics.drawString(font, pages, panelLeft + panelWidth - PADDING / 2 - font.width(pages),
                    panelTop + panelHeight - 10, 0x777777);
        }

        if (hovered >= 0) {
            graphics.renderComponentTooltip(font, tooltipFor(entries.get(hovered)), mouseX, mouseY);
        }
    }

    private void renderSpell(GuiGraphics graphics, BoundSpellsPayload.Entry entry, int x, int y, boolean hovered) {
        boolean enabled = entry.enabled();

        // Outline
        graphics.fill(x, y, x + CELL, y + CELL, entry.fromSet() ? SET_COLOR : enabled ? ENABLED_COLOR : DISABLED_COLOR);
        graphics.fill(x + BORDER, y + BORDER, x + CELL - BORDER, y + CELL - BORDER, CELL_BACKGROUND);

        int iconX = x + BORDER;
        int iconY = y + BORDER;
        AbstractSpell spell = SpellRegistry.getSpell(entry.spell());
        if (spell != null && spell != SpellRegistry.none()) {
            ResourceLocation icon = spell.getSpellIconResource();
            if (!enabled) icon = greyscale(icon);
            graphics.blit(icon, iconX, iconY, ICON, ICON, 0, 0, 16, 16, 16, 16);
        } else {
            graphics.drawCenteredString(font, "?", iconX + ICON / 2, iconY + ICON / 2 - 4, 0xFF5555);
        }

        if (!enabled) {
            for (int i = 0; i < ICON; i++) {
                graphics.fill(iconX + i, iconY + i, Math.min(iconX + i + 2, iconX + ICON), Math.min(iconY + i + 2, iconY + ICON), DISABLED_COLOR);
            }
        }

        if (hovered) {
            graphics.fill(iconX, iconY, iconX + ICON, iconY + ICON, HOVER_TINT);
        }
    }

    private List<Component> tooltipFor(BoundSpellsPayload.Entry entry) {
        List<Component> lines = new ArrayList<>();
        AbstractSpell spell = SpellRegistry.getSpell(entry.spell());
        if (spell == null || spell == SpellRegistry.none()) {
            lines.add(Component.literal(entry.spell().toString()).withStyle(ChatFormatting.RED));
            return lines;
        }

        Player player = Minecraft.getInstance().player;
        LivingEntity familiar = findBoundFamiliar();
        LivingEntity caster = familiar != null ? familiar : player;
        int level = entry.level();
        SpellRarity rarity = spell.getRarity(level);

        lines.add(spell.getDisplayName(player).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.literal(" ").append(Component.translatable("tooltip.irons_spellbooks.level", level)
                .append(" ")
                .append(Component.translatable("tooltip.irons_spellbooks.rarity", rarity.getDisplayName()).withStyle(rarity.getDisplayName().getStyle()))
                .withStyle(ChatFormatting.GRAY)));
        for (MutableComponent line : spell.getUniqueInfo(level, caster)) {
            lines.add(Component.literal(" ").append(line.withStyle(line.getStyle().applyTo(TooltipsUtils.getStyleFor(player, spell)))));
        }
        if (spell.getCastType() != CastType.INSTANT) {
            lines.add(Component.literal(" ").append(TooltipsUtils.getCastTimeComponent(spell.getCastType(),
                    Utils.timeFromTicks(spell.getEffectiveCastTime(level, caster), 2)).withStyle(ChatFormatting.BLUE)));
        }
        lines.add(Component.empty());
        lines.add(spell.getSchoolType().getDisplayName().copy());
        lines.add(Component.empty());
        if (entry.fromSet()) {
            lines.add(Component.translatable("ui.origins_familiarity.spells.from_set").withStyle(ChatFormatting.GOLD));
        } else {
            lines.add(entry.enabled()
                    ? Component.translatable("ui.origins_familiarity.spells.enabled").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("ui.origins_familiarity.spells.disabled").withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    /** The player's bound familiar if it's summoned and loaded nearby, so tooltip numbers use its spell power. */
    @Nullable
    private LivingEntity findBoundFamiliar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        UUID owner = mc.player.getUUID();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof IBoundFamiliar && entity instanceof AbstractSpellCastingPet pet && owner.equals(pet.getOwnerUUID())) {
                return pet;
            }
        }
        return null;
    }

    /** A cached greyscale copy of a spell icon (falls back to the original if it can't be read). */
    private static ResourceLocation greyscale(ResourceLocation icon) {
        return GREYSCALE_ICONS.computeIfAbsent(icon, id -> {
            Minecraft mc = Minecraft.getInstance();
            try (InputStream in = mc.getResourceManager().open(id)) {
                NativeImage image = NativeImage.read(in);
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int pixel = image.getPixelRGBA(x, y); // ABGR
                        int a = (pixel >>> 24) & 0xFF;
                        int b = (pixel >>> 16) & 0xFF;
                        int g = (pixel >>> 8) & 0xFF;
                        int r = pixel & 0xFF;
                        int grey = (int) ((0.299 * r + 0.587 * g + 0.114 * b) * 0.8); // slightly dimmed
                        image.setPixelRGBA(x, y, (a << 24) | (grey << 16) | (grey << 8) | grey);
                    }
                }
                ResourceLocation greyId = OriginsFamiliarity.id("greyscale/" + id.getNamespace() + "/" + id.getPath());
                mc.getTextureManager().register(greyId, new DynamicTexture(image));
                return greyId;
            } catch (IOException e) {
                return id;
            }
        });
    }

    // Input

    private int cellX(int col) {
        return gridLeft + col * (CELL + GAP);
    }

    private int cellY(int row) {
        return gridTop + row * (CELL + GAP);
    }

    /** Index of the spell under the mouse, or -1. */
    private int indexAt(double mouseX, double mouseY) {
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollRow + row) * COLUMNS + col;
                if (index >= entries.size()) return -1;
                int x = cellX(col);
                int y = cellY(row);
                if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                    return index;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int index = indexAt(mouseX, mouseY);
            if (index >= 0) {
                BoundSpellsPayload.Entry entry = entries.get(index);
                if (entry.fromSet()) return true;
                boolean enabled = !entry.enabled();
                entries.set(index, new BoundSpellsPayload.Entry(entry.spell(), entry.level(), enabled, false));
                PacketDistributor.sendToServer(new ToggleBoundSpellPayload(entry.spell(), enabled));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && maxScrollRow() > 0) {
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
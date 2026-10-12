package com.sweetberry.gui;

import com.sweetberry.Category;
import com.sweetberry.Module;
import com.sweetberry.Setting;
import com.sweetberry.SweetBerryClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Sweet-Berry-GUI: Panel pro Kategorie, Linksklick = an/aus, Rechtsklick = Einstellungen. */
public class ClickGuiScreen extends Screen {
    // Beeren-Farbschema
    private static final int BG        = 0xE61B0F24;
    private static final int HEADER    = 0xFFB0327A;
    private static final int ROW       = 0xCC2A1836;
    private static final int ROW_HOVER = 0xCC3A2248;
    private static final int ON        = 0xFFE0508F;
    private static final int TEXT      = 0xFFF3E6F5;
    private static final int DIM       = 0xFF9C84A6;
    private static final int SLIDER_BG = 0xFF140A1B;

    private static final int PANEL_W = 130, ROW_H = 16, HEADER_H = 20, SET_H = 14;

    private final Set<Module> expanded = new HashSet<>();
    private Setting dragging;
    private int dragX, dragW;

    public ClickGuiScreen() { super(Text.literal("Sweet Berry Client")); }

    @Override public boolean shouldPause() { return false; }

    private int panelX(int index) { return 20 + index * (PANEL_W + 10); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        ctx.drawText(textRenderer, "§dSweet Berry §fClient", 20, 6, TEXT, true);

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int x = panelX(i), y = 24;
            List<Module> mods = SweetBerryClient.modules.byCategory(cats[i]);

            int h = HEADER_H;
            for (Module m : mods) { h += ROW_H; if (expanded.contains(m)) h += m.settings.size() * SET_H; }
            ctx.fill(x, y, x + PANEL_W, y + h, BG);
            ctx.fill(x, y, x + PANEL_W, y + HEADER_H, HEADER);
            ctx.drawText(textRenderer, cats[i].label, x + 6, y + 6, 0xFFFFFFFF, true);

            int cy = y + HEADER_H;
            for (Module m : mods) {
                boolean hover = mx >= x && mx < x + PANEL_W && my >= cy && my < cy + ROW_H;
                ctx.fill(x, cy, x + PANEL_W, cy + ROW_H, hover ? ROW_HOVER : ROW);
                if (m.isEnabled()) ctx.fill(x, cy, x + 2, cy + ROW_H, ON);
                ctx.drawText(textRenderer, m.name, x + 7, cy + 4, m.isEnabled() ? ON : TEXT, true);
                ctx.drawText(textRenderer, expanded.contains(m) ? "-" : "+", x + PANEL_W - 10, cy + 4, DIM, false);
                cy += ROW_H;

                if (expanded.contains(m)) {
                    for (Setting s : m.settings) {
                        if (s.isBool) {
                            ctx.drawText(textRenderer, s.name, x + 8, cy + 3, DIM, false);
                            ctx.drawText(textRenderer, s.getBool() ? "§aON" : "§cOFF", x + PANEL_W - 28, cy + 3, TEXT, false);
                        } else {
                            int sx = x + 8, sw = PANEL_W - 16;
                            ctx.fill(sx, cy, sx + sw, cy + SET_H - 2, SLIDER_BG);
                            ctx.fill(sx, cy, sx + (int) (sw * s.percent()), cy + SET_H - 2, ON);
                            String label = s.name + ": " + s.getInt();
                            ctx.drawText(textRenderer, label, sx + 3, cy + 2, TEXT, false);
                        }
                        cy += SET_H;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int x = panelX(i), cy = 24 + HEADER_H;
            if (mx < x || mx >= x + PANEL_W) continue;
            for (Module m : SweetBerryClient.modules.byCategory(cats[i])) {
                if (my >= cy && my < cy + ROW_H) {
                    if (button == 0) m.toggle();
                    else if (button == 1) { if (!expanded.remove(m)) expanded.add(m); }
                    return true;
                }
                cy += ROW_H;
                if (expanded.contains(m)) {
                    for (Setting s : m.settings) {
                        if (my >= cy && my < cy + SET_H) {
                            if (s.isBool) { s.value = s.getBool() ? 0 : 1; }
                            else { dragging = s; dragX = x + 8; dragW = PANEL_W - 16; s.setFromPercent((mx - dragX) / dragW); }
                            return true;
                        }
                        cy += SET_H;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { dragging.setFromPercent((mx - dragX) / dragW); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }
}

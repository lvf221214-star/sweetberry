package com.sweetberry.module;

import com.sweetberry.Category;
import com.sweetberry.Module;
import com.sweetberry.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Scannt geladene Chunks nach Amethyst-Geoden und Strukturen, die auf Spieleraktivität hindeuten. */
public class ChunkFinder extends Module {
    private final Setting sensitivity = add(Setting.number("Sensitivity", 5, 1, 10, 1));
    private final Setting simDistance = add(Setting.number("Simulation Distance", 8, 2, 32, 1));
    private final Setting render      = add(Setting.bool("Render", true));
    private final Setting chat        = add(Setting.bool("Chat Notify", true));

    private record Hit(int score, int y, boolean geode) {}
    private final Map<ChunkPos, Hit> hits = new HashMap<>();
    private final Set<ChunkPos> notified = new HashSet<>();
    private int ticks;

    public ChunkFinder() {
        super("Chunk Finder", "Findet Geoden & Spieleraktivität in Chunks", Category.DONUT);
    }

    @Override public void onEnable()  { hits.clear(); notified.clear(); ticks = 0; }
    @Override public void onDisable() { hits.clear(); notified.clear(); }

    @Override
    public String hudInfo() { return hits.isEmpty() ? null : String.valueOf(hits.size()); }

    private static boolean isAmethyst(BlockState s) {
        return s.isOf(Blocks.BUDDING_AMETHYST) || s.isOf(Blocks.AMETHYST_BLOCK)
            || s.isOf(Blocks.AMETHYST_CLUSTER) || s.isOf(Blocks.LARGE_AMETHYST_BUD);
    }

    private static boolean isPlayerBlock(BlockState s) {
        Block b = s.getBlock();
        return b == Blocks.CRAFTING_TABLE || b == Blocks.ANVIL || b == Blocks.BEACON
            || b == Blocks.ENCHANTING_TABLE || b == Blocks.SMITHING_TABLE || b == Blocks.BREWING_STAND
            || b == Blocks.CONDUIT || b == Blocks.RESPAWN_ANCHOR;
    }

    private static int blockEntityWeight(BlockEntity be) {
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity
            || be instanceof ShulkerBoxBlockEntity || be instanceof EnderChestBlockEntity) return 2;
        if (be instanceof HopperBlockEntity || be instanceof AbstractFurnaceBlockEntity
            || be instanceof BedBlockEntity || be instanceof DispenserBlockEntity) return 1;
        return 0;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        if (++ticks % 40 != 0) return;

        int radius = Math.min(simDistance.getInt(), mc.options.getViewDistance().getValue());
        int threshold = 11 - sensitivity.getInt();           // hohe Sensitivity = niedrige Schwelle
        ChunkPos center = mc.player.getChunkPos();
        hits.clear();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(center.x + dx, center.z + dz);
                if (chunk == null) continue;

                int score = 0, y = mc.player.getBlockY();
                boolean geode = false;

                ChunkSection[] sections = chunk.getSectionArray();
                for (int i = 0; i < sections.length; i++) {
                    ChunkSection sec = sections[i];
                    if (sec == null || sec.isEmpty()) continue;
                    int secY = chunk.getBottomY() + i * 16;
                    if (!geode && sec.hasAny(ChunkFinder::isAmethyst)) { geode = true; score += 3; y = secY; }
                    if (sec.hasAny(ChunkFinder::isPlayerBlock)) { score += 1; y = secY; }
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    int w = blockEntityWeight(be);
                    if (w > 0) { score += w; y = be.getPos().getY(); }
                }

                if (score >= threshold) {
                    ChunkPos cp = chunk.getPos();
                    hits.put(cp, new Hit(score, y, geode));
                    if (chat.getBool() && notified.add(cp)) {
                        mc.player.sendMessage(Text.literal("§d[Sweet Berry] §fChunk Finder: §e"
                            + (geode ? "Geode" : "Aktivität") + " §fbei §b" + cp.getStartX() + ", " + cp.getStartZ()
                            + " §7(Score " + score + ")"), false);
                    }
                }
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext ctx) {
        if (!render.getBool() || hits.isEmpty() || ctx.consumers() == null) return;
        Vec3d cam = ctx.camera().getPos();
        VertexConsumer lines = ctx.consumers().getBuffer(RenderLayer.getLines());
        var matrices = ctx.matrixStack();

        for (Map.Entry<ChunkPos, Hit> e : hits.entrySet()) {
            ChunkPos cp = e.getKey();
            Hit h = e.getValue();
            double x1 = cp.getStartX() - cam.x, z1 = cp.getStartZ() - cam.z;
            double y1 = h.y() - cam.y;
            // Geode = lila, Spieleraktivität = pink
            float r = h.geode() ? 0.7f : 1.0f, g = h.geode() ? 0.3f : 0.2f, b = h.geode() ? 1.0f : 0.5f;
            WorldRenderer.drawBox(matrices, lines, x1, y1, z1, x1 + 16, y1 + 16, z1 + 16, r, g, b, 1f);
        }
    }
}

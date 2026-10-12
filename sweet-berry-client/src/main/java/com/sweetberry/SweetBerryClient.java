package com.sweetberry;

import com.sweetberry.gui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SweetBerryClient implements ClientModInitializer {
    public static ModuleManager modules;
    private static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.sweetberry.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.sweetberry"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) client.setScreen(new ClickGuiScreen());
            for (Module m : modules.modules) if (m.isEnabled()) m.onTick();
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            for (Module m : modules.modules) if (m.isEnabled()) m.onWorldRender(ctx);
        });

        // HUD: Wasserzeichen + aktive Module
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden) return;
            ctx.drawText(mc.textRenderer, "§dSweet Berry §fClient", 4, 4, 0xFFFFFFFF, true);
            int y = 16;
            for (Module m : modules.modules) {
                if (!m.isEnabled()) continue;
                String info = m.hudInfo();
                ctx.drawText(mc.textRenderer, m.name + (info != null ? " §7[" + info + "]" : ""), 4, y, 0xFFE0508F, true);
                y += 10;
            }
        });
    }
}

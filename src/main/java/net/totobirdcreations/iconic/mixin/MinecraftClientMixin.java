package net.totobirdcreations.iconic.mixin;

import net.minecraft.client.MinecraftClient;
import net.totobirdcreations.iconic.IconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ticks animation frames.
 */

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void tickIconAnims(boolean tick, CallbackInfo ci) {
        IconRenderer.delta();
    }

}

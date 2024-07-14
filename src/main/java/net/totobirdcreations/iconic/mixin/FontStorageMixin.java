package net.totobirdcreations.iconic.mixin;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Failsafe.
 */

@Mixin(FontStorage.class)
abstract class FontStorageMixin {

    @Redirect(method = "getGlyphRenderer(Lnet/minecraft/client/font/RenderableGlyph;)Lnet/minecraft/client/font/GlyphRenderer;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;registerTexture(Lnet/minecraft/util/Identifier;Lnet/minecraft/client/texture/AbstractTexture;)V"))
    private void cancelRegisterIfNull(@Nullable TextureManager instance, Identifier id, AbstractTexture texture) {
        if (instance != null) { instance.registerTexture(id, texture); }
    }

}

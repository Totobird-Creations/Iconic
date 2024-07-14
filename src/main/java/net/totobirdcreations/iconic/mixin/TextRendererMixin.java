package net.totobirdcreations.iconic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import net.totobirdcreations.iconic.IconCache;
import net.totobirdcreations.iconic.Iconic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

/**
 * Hijack the font system.
 */

@Mixin(TextRenderer.class)
abstract class TextRendererMixin {

    @WrapOperation(method = "getFontStorage", at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object iconicFont(Function<?, ?> instance, Object idt, Operation<Object> original) {
        Identifier id = (Identifier) idt;
        if (id.getNamespace().equals(Iconic.ID)) {
            try {
                return IconCache.getRemoteFontStorage(id.getPath(), (FontStorage) original.call(instance, new Identifier("default")));
            } catch (NumberFormatException ignored) {}
        }
        return original.call(instance, idt);
    }

}

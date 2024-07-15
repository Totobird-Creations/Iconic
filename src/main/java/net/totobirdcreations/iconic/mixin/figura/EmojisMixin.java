package net.totobirdcreations.iconic.mixin.figura;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.totobirdcreations.iconic.ChatScanner;
import org.figuramc.figura.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents Figura from messing with Iconic icons, while still allowing Figura icons to function.
 */

@Mixin(Emojis.class)
abstract class EmojisMixin {

    @Shadow
    private static MutableText convertEmoji(String string, Style style) { throw new AssertionError(); }

    @Unique private static boolean alreadyHandling = false;

    @Inject(method = "convertEmoji", at = @At("HEAD"), cancellable = true)
    private static void disableFiguraEmojis(String message, Style style, CallbackInfoReturnable<MutableText> cir) {
        if (! alreadyHandling) {
            MutableText text = Text.empty();
            boolean isIcon = false;
            for (String component : ChatScanner.splitIcons(message)) {
                if (isIcon) {
                    text.append("<:" + component + ":>");
                } else {
                    alreadyHandling = true;
                    MutableText emojis = convertEmoji(component, style);
                    alreadyHandling = false;
                    text.append(emojis);
                }
                isIcon = ! isIcon;
            }
            cir.setReturnValue(text);
        }
    }

}

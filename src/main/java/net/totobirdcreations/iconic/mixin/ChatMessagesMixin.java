package net.totobirdcreations.iconic.mixin;

import net.minecraft.client.util.ChatMessages;
import net.minecraft.client.util.TextCollector;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.totobirdcreations.iconic.ChatScanner;
import net.totobirdcreations.iconic.IconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Scan for icons in incoming messages.
 */

@Mixin(ChatMessages.class)
abstract class ChatMessagesMixin {

    @Shadow
    private static String getRenderedChatMessage(String message) { throw new AssertionError(); }

    @Inject(method = "method_27536", at = @At("HEAD"), remap = false, cancellable = true)
    private static void separateIcons(TextCollector collector, Style style, String message, CallbackInfoReturnable<Optional> cir) {
        boolean isIcon = false;
        for (String component : ChatScanner.splitIcons(message)) {
            Style currentStyle = style;
            if (isIcon) {
                currentStyle = IconRenderer.applyStyle(component, currentStyle);
                component = " ";
            }
            collector.add(StringVisitable.styled(getRenderedChatMessage(component), currentStyle));
            isIcon = ! isIcon;
        }
        cir.setReturnValue(Optional.empty());
    }

}

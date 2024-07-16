package net.totobirdcreations.iconic.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.totobirdcreations.iconic.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a new click event open url protocol which opens the icon view menu.
 */

@Mixin(Screen.class)
abstract class ScreenMixin {

    @Shadow @Nullable protected MinecraftClient client;

    @Inject(method = "handleTextClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 0, shift = At.Shift.BEFORE), cancellable = true)
    private void expandIcon(Style style, CallbackInfoReturnable<Boolean> cir, @Local ClickEvent clickEvent) {
        String uri = clickEvent.getValue();;
        String prefix = Iconic.ID + "://";
        if (uri.startsWith(prefix)) {
            String target = uri.substring(prefix.length());
            Pair<String, String> parts = IconTransporter.getFileNameParts(target, ":");
            String name        = parts.component1();
            String transportId = parts.component2();
            assert this.client != null;
            this.client.setScreen(new IconViewScreen(transportId, name,
                    (! (name.startsWith("#") && IconCache.getDefaultIconIconIds().contains(name)))
                            ? IconRenderer.IconNamespace.IconicBuiltin
                            : IconRenderer.IconNamespace.Iconic
            ));
            cir.setReturnValue(true);
        }
    }

}

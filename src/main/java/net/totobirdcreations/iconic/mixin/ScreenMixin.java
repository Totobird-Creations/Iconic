package net.totobirdcreations.iconic.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.totobirdcreations.iconic.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
        if (       this.tryCase(uri, Iconic.ID, this::createIconicScreen)
                || this.tryCase(uri, "figura", this::createFiguraScreen)
        ) { cir.setReturnValue(true); }
    }

    @Unique
    private boolean tryCase(String uri, String id, IconViewGetter screen) {
        String prefix = id + "://";
        if (uri.startsWith(prefix)) {
            String data = uri.substring(prefix.length());
            assert this.client != null;
            screen.get(data).open();
            return true;
        }
        return false;
    }


    @Unique
    private IconViewScreen createIconicScreen(String data) {
        Pair<String, String> parts = IconTransporter.getFileNameParts(data, ":");
        String name        = parts.component1();
        String transportId = parts.component2();
        return new IconViewScreen(transportId, name,
                (name.startsWith("#") && IconCache.getDefaultIconIconIds().contains(name))
                        ? IconRenderer.IconNamespace.IconicBuiltin
                        : IconRenderer.IconNamespace.Iconic
        );
    }
    @Unique
    private IconViewScreen.Figura createFiguraScreen(String data) {
        Pair<String, String> parts = IconTransporter.getFileNameParts(data, ":");
        String name = parts.component2();
        parts = IconTransporter.getFileNameParts(parts.component1(), ":");
        String group = parts.component2();
        parts = IconTransporter.getFileNameParts(parts.component1(), ":");
        String unicode = parts.component2();
        Identifier font = new Identifier(parts.component1());
        return new IconViewScreen.Figura(
                name, group, unicode, font
        );
    }

}

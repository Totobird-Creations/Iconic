package net.totobirdcreations.iconic.mixin.figura;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.totobirdcreations.iconic.IconRenderer;
import org.figuramc.figura.font.EmojiContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(EmojiContainer.class)
abstract class FiguraEmojiContainerMixin {

    @Shadow(remap = false) @Final public String name;

    @Shadow(remap = false) @Final private Identifier font;

    @WrapOperation(method = "getEmojiComponent(Ljava/lang/String;Lnet/minecraft/text/MutableText;)Lnet/minecraft/text/Text;", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/font/EmojiContainer;makeComponent(Ljava/lang/String;Lnet/minecraft/text/MutableText;)Lnet/minecraft/text/Text;"))
    private Text getEmojiComponent(EmojiContainer instance, String unicode, MutableText hover, Operation<Text> original, @Local(argsOnly = true) String key) {
        return this.createComponent(unicode, key);
    }

    @WrapOperation(method = "getShortcutComponent", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/font/EmojiContainer;makeComponent(Ljava/lang/String;Lnet/minecraft/text/MutableText;)Lnet/minecraft/text/Text;"))
    private Text getShortcutComponent(EmojiContainer instance, String unicode, MutableText hover, Operation<Text> original, @Local(argsOnly = true) String shortcut) {
        return this.createComponent(unicode, shortcut);
    }

    @Unique
    private Text createComponent(String unicode, String key) {
        return Text.literal(unicode).styled((s) -> s.withFont(this.font).withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        IconRenderer.createStyleHoverEvent(key, IconRenderer.IconNamespace.Figura, "emoji." + this.name + "." + key, false)
                )
        ));
    }

}

package net.totobirdcreations.iconic.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.totobirdcreations.iconic.IconCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * Adds a button to the chat screen which opens the icon directory.
 */

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin extends Screen {
    protected ChatScreenMixin() { super(Text.empty()); }

    @Unique
    private PressableTextWidget iconsButton;

    @ModifyConstant(method = "init", constant = @Constant(intValue = 4, ordinal = 1))
    private int shrinkTextField(int constant) {
        return 20;
    }
    @Inject(method = "init", at = @At(value = "RETURN"))
    private void prepareIconsButton(CallbackInfo ci) {
        if (this.iconsButton == null) {
            this.iconsButton = new PressableTextWidget(
                    0, 0, 12, 12,
                    Text.literal("★"), (button -> {
                        File dir = IconCache.getLOCAL_ICONS_PATH();
                        if (! dir.isDirectory()) {
                            //noinspection ResultOfMethodCallIgnored
                            dir.mkdirs();
                        }
                        Util.getOperatingSystem().open(dir);
                    }), this.textRenderer
            );
        }
        this.iconsButton.setPosition(this.width - 12, this.height - 12);
        this.iconsButton.setWidth(8);
        this.iconsButton.setHeight(8);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void clickIconsButton(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.iconsButton.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 2, ordinal = 1))
    private int shrinkTextFieldBackground(int constant) {
        return 16;
    }
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER))
    private void renderIconsButtonBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        assert this.client != null;
        context.fill(this.width - 14, this.height - 14, this.width - 2, this.height - 2, this.client.options.getTextBackgroundColor(Integer.MIN_VALUE));
        this.iconsButton.render(context, mouseX, mouseY, delta);
    }

}

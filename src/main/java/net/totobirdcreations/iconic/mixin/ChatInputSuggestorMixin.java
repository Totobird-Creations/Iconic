package net.totobirdcreations.iconic.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import kotlin.Pair;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.totobirdcreations.iconic.ChatScanner;
import net.totobirdcreations.iconic.Iconic;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Suggest icons from the local icon directory.
 */

@Mixin(ChatInputSuggestor.class)
abstract class ChatInputSuggestorMixin {

    @Shadow private @Nullable CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow @Final TextFieldWidget textField;

    @Shadow public abstract void show(boolean narrateFirstSuggestion);

    @Shadow public abstract void clearWindow();

    @Inject(method = "refresh", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getText()Ljava/lang/String;", shift = At.Shift.AFTER), cancellable = true)
    private void addIconSuggestions(CallbackInfo ci, @Local(ordinal = 0) String string) {
        int cursor = this.textField.getCursor();
        if (cursor == 0) { return; }
        String before = string.substring(0, cursor);
        Pair<String, Integer> prefix = ChatScanner.findSuggestionPrefix(string);
        if (prefix != null) {
            String  prefixWord = prefix.component1();
            Integer prefixLen  = prefix.component2();
            this.pendingSuggestions = CompletableFuture.supplyAsync(() -> {
                StringRange range = StringRange.between(before.length() - prefixLen - ChatScanner.OUTGOING_PREFIX.length(), before.length());
                ArrayList<Suggestion> suggestions = new ArrayList<>();
                for (String suggestion : ChatScanner.getSuggestions(prefixWord)) {
                    suggestions.add(new Suggestion(range, ChatScanner.OUTGOING_PREFIX + suggestion + ChatScanner.OUTGOING_SUFFIX));
                }
                return Suggestions.create(string, suggestions);
            });
            this.pendingSuggestions.whenComplete((suggestions, exception) -> {
                if (suggestions.isEmpty()) {
                    this.clearWindow();
                    this.textField.setSuggestion("");
                } else {
                    this.show(false);
                }
            } );
            ci.cancel();
        }
    }

}

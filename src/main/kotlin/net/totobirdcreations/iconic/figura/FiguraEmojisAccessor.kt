package net.totobirdcreations.iconic.figura

import net.fabricmc.loader.api.FabricLoader
import org.figuramc.figura.font.EmojiContainer
import org.figuramc.figura.font.Emojis


object FiguraEmojisAccessor {

    fun getEmojiNames() : Set<String> {
        return if (FabricLoader.getInstance().isModLoaded("figura")) {
            val field = Emojis::class.java.getDeclaredField("EMOJIS");
            field.isAccessible = true;
            val iconNames = mutableSetOf<String>();
            for (container in (field.get(null) as Map<String, EmojiContainer>).values) {
                iconNames.addAll(container.lookup.names);
            }
            iconNames
        } else { setOf() };
    }

}
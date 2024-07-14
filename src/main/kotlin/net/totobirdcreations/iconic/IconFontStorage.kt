package net.totobirdcreations.iconic

import net.minecraft.client.font.*
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

class IconFontStorage(
    private val renderer    : GlyphRenderer,
    private val glyph       : Glyph,
    private val defaultFont : FontStorage
) : FontStorage(null, Identifier("default")) {

    init {
        this.blankGlyphRenderer          = BuiltinEmptyGlyph.MISSING .bake(this::superGetGlyphRenderer);
        this.whiteRectangleGlyphRenderer = BuiltinEmptyGlyph.WHITE   .bake(this::superGetGlyphRenderer);
    }

    override fun findGlyph(codePoint : Int) : GlyphPair {
        return GlyphPair(this.glyph, this.glyph);
    }
    override fun getGlyph(codePoint: Int, validateAdvance: Boolean): Glyph { return this.glyph; }

    override fun findGlyphRenderer(codePoint: Int) : GlyphRenderer { return this.renderer; }
    override fun getGlyphRenderer(codePoint: Int) : GlyphRenderer { return this.renderer; }
    override fun getGlyphRenderer(c : RenderableGlyph) : GlyphRenderer { return this.renderer; }
    private fun superGetGlyphRenderer(c : RenderableGlyph) : GlyphRenderer {
        return super.getGlyphRenderer(c);
    }

    override fun getObfuscatedGlyphRenderer(glyph : Glyph) : GlyphRenderer {
        val chars = this.defaultFont.charactersByWidth[MathHelper.ceil(glyph.getAdvance(false))];
        return if (chars != null && chars.isNotEmpty()) {
            super.getGlyphRenderer(chars.getInt(RANDOM.nextInt(chars.size)));
        } else {
            this.blankGlyphRenderer;
        };
    }

}
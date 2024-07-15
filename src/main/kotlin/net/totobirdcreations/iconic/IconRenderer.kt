package net.totobirdcreations.iconic

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.font.Glyph
import net.minecraft.client.font.GlyphRenderer
import net.minecraft.client.font.RenderableGlyph
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import java.util.function.Function
import kotlin.math.floor


object IconRenderer {

    private const val ANIM_CYCLE : Float = Float.MAX_VALUE / 2.0f;
    private var lastTime : Long  = System.currentTimeMillis();
    private var animTime : Float = 0.0f;
    @JvmStatic
    fun delta() {
        val currentTime = System.currentTimeMillis();
        this.animTime = (this.animTime + (currentTime - this.lastTime).toFloat() / 37.5f).rem(ANIM_CYCLE);
        this.lastTime = currentTime;
    }

    @JvmStatic
    fun applyStyle(target : String, style : Style) : Style {
        val (name, id) = IconTransporter.getFileNameParts(target, ":");
        return style
            .withColor(Formatting.WHITE)
            .withBold(false)
            .withFont(Identifier(Iconic.ID, id))
            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, run {
                val text = Text.empty()
                    .append(Text.literal(name                  ).styled{ s -> s.withBold(true).withColor(Formatting.WHITE) })
                    .append(Text.literal("\nIconic ${id}"      ).styled{ s -> s.withColor(Formatting.DARK_GRAY) })
                    .append(Text.literal("\n\nᴄʟɪᴄᴋ ᴛᴏ ᴇxᴘᴀɴᴅ" ).styled{ s -> s.withColor(Formatting.YELLOW) });
                text
            }))
            .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "${Iconic.ID}://${name}:${id}"));
    }



    class IconGlyph(private val renderer : GlyphRenderer) : Glyph {
        override fun getAdvance() : Float { return 8.0f; }
        override fun bake(getter : Function<RenderableGlyph, GlyphRenderer>) : GlyphRenderer {
            return this.renderer;
        }
    }
    class IconGlyphRenderer(private val frames : Int):
        GlyphRenderer(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    {
        var iconGlID : Int? = null;
        private val framesF : Float = this.frames.toFloat();

        override fun draw(
            italic : Boolean,
            x : Float, y : Float,
            mat : Matrix4f,
            vcs : VertexConsumer,
            r : Float, g : Float, b : Float, a : Float, l : Int
        ) { this.draw(x, y, 8.0f, 8.0f, mat, r, g, b, a); }

        fun draw(
            x : Float, y : Float,
            w : Float, h : Float,
            mat : Matrix4f,
            r : Float, g : Float, b : Float, a : Float
        ) {
            val iconGlID = this.iconGlID ?: return;
            @Suppress("RemoveRedundantQualifierName")
            val frame = floor(IconRenderer.animTime % this.framesF);
            val x1 = x + w;
            val y1 = y + h;
            val z  = 0.0f;
            // Set up render system.
            RenderSystem.setShaderTexture(0, iconGlID);
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(r, g, b, a);
            // Set up buffer.
            val bb = Tessellator.getInstance().buffer;
            bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
            // Draw mesh.
            bb.vertex(mat, x  , y  , z).color(r, g, b, a).texture(0.0f, (frame        ) / this.framesF).next();
            bb.vertex(mat, x  , y1 , z).color(r, g, b, a).texture(0.0f, (frame + 1.0f ) / this.framesF).next();
            bb.vertex(mat, x1 , y1 , z).color(r, g, b, a).texture(1.0f, (frame + 1.0f ) / this.framesF).next();
            bb.vertex(mat, x1 , y  , z).color(r, g, b, a).texture(1.0f, (frame        ) / this.framesF).next();
            // Close buffer.
            BufferRenderer.drawWithGlobalProgram(bb.end());
            // Reset render system.
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        override fun getLayer(type : TextRenderer.TextLayerType) : RenderLayer {
            return RenderLayer.getGui();
        }
    }

}
package net.totobirdcreations.iconic.generator

import com.google.common.primitives.Ints.min
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.totobirdcreations.iconic.ChatScanner
import net.totobirdcreations.iconic.IconCache
import net.totobirdcreations.iconic.IconCache.LOCAL_ICONS_PATH
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil


object PetPetGenerator : IconGenerator() {

    private val OVERLAY : BufferedImage = ImageIO.read(IconCache.getInternalIconStream("generator/petpet"));
    private val WIDTH   : Int           = OVERLAY.width;
    private val HEIGHT  : Int           = OVERLAY.height;
    private val FRAMES  : Int           = HEIGHT / WIDTH;
    private val WIDTHF  : Float         = OVERLAY.width.toFloat();
    private val FRAMESF : Float         = OVERLAY.height.toFloat() / WIDTHF;

    private const val SCALE : Float = 0.875f;

    override val name : String = "petpet";

    override fun addArguments(root : LiteralArgumentBuilder<S>) {
        root.then(argument("baseIcon", StringArgumentType.word())
            .suggests{ _, b -> CompletableFuture.supplyAsync{ ->
                for (s in ChatScanner.getSuggestions("")) {
                    b.suggest(s)
                }
                b.build()
            } }
            .executes{ ctx -> this.generatePetPet(StringArgumentType.getString(ctx, "baseIcon")); 0 }
        );
    }

    private fun generatePetPet(baseIconName : String) {
        val baseIcon = try {
            ImageIO.read(LOCAL_ICONS_PATH.resolve("${baseIconName}.png"))
        } catch (e : Exception) { throwError("Failed to load icon `${baseIconName}`: ${e.message ?: e}."); }
            ?: throwError("Failed to load icon `${baseIconName}`: Could not create image.");

        val otherFrames = baseIcon.height / baseIcon.width;
        val totalFrames = min(Util.lcm(FRAMES, otherFrames).also{ f ->
            if (f > 100) {
                MinecraftClient.getInstance().player?.sendMessage(Text.literal("Warning: The resulting image is too large. Limiting to 100 frames.").formatted(Formatting.GOLD));
            }
        }, 100);
        val image = BufferedImage(WIDTH, WIDTH * totalFrames, IconCache.FORMAT);

        val fracFrames2 = ceil(FRAMESF / 2.0f);
        val g = image.createGraphics();
        for (j in 0..<totalFrames) {
            val i = j % FRAMES;
            val y = j.toFloat() * WIDTHF;
            val w0 = WIDTHF * SCALE;
            val w1 = WIDTHF * (1.0f - SCALE);
            val squish = (1.0f - abs(fracFrames2 - i.toFloat()) / fracFrames2) * (w0 * 0.25f);
            g.drawImage(
                Util.scaleBuffered(
                    Util.cropBuffered(baseIcon,
                        0, baseIcon.width * (j % otherFrames),
                        baseIcon.width, baseIcon.width
                    ),
                    w0.toInt(),
                    (w0 - squish).toInt()
                ),
                w1.toInt(), ceil(y + w1 + squish).toInt(),
                null
            );
        }
        for (i in 0..<totalFrames) {
            if (i % FRAMES == 0) {
                g.drawImage(OVERLAY, 0, WIDTH * i, null);
            }
        }

        g.dispose();
        saveIcon(Util.imageToBuffered(image), "petpet_${baseIconName}");
    }

}
package net.totobirdcreations.iconic.generator

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.totobirdcreations.iconic.IconCache
import net.totobirdcreations.iconic.IconTransporter
import net.totobirdcreations.iconic.Iconic
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max


typealias S = FabricClientCommandSource;


abstract class IconGenerator {

    companion object {
        fun registerCommand() {
            ClientCommandRegistrationCallback.EVENT.register{ d, _ ->
                val root = literal(Iconic.ID);

                this.registerSubCommand(root, PlayerSkinGenerator);
                this.registerSubCommand(root, PetPetGenerator);
                this.registerSubCommand(root, UrlGenerator);

                d.register(root);
            };
        }
        private fun registerSubCommand(root : LiteralArgumentBuilder<S>, generator : IconGenerator) {
            val subRoot = literal(generator.name);
            generator.addArguments(subRoot);
            root.then(subRoot);
        }

        fun throwError(msg : String) : Nothing {
            Iconic.LOGGER.error(msg);
            throw SimpleCommandExceptionType(Text.literal(msg)).create();
        }

        fun findSaveLocation(name : String) : File {
            var file = IconCache.LOCAL_ICONS_PATH.resolve("${name}.png");
            var i = 0;
            while (file.exists()) {
                file = IconCache.LOCAL_ICONS_PATH.resolve("${name}${i}.png");
                i += 1;
            }
            return file;
        }
        fun saveIcon(icon : BufferedImage, name : String) {
            val file = this.findSaveLocation(name);
            try { ImageIO.write(icon, "png", file); }
            catch (e : Exception) {
                this.throwError("Failed to save icon `${file.nameWithoutExtension}`: ${e.message ?: e.toString()}");
            }
            MinecraftClient.getInstance().player?.sendMessage(Text.literal("Created icon `${file.nameWithoutExtension}`.").styled{ s -> s.withColor(Formatting.YELLOW) });
        }

    }
    object Util {

        fun bufferedToNative(buffered : BufferedImage) : NativeImage {
            val native = NativeImage(NativeImage.Format.RGBA, buffered.width, buffered.height, false);
            for (y in 0..<buffered.height) {
                for (x in 0..<buffered.width) {
                    native.setColor(x, y, buffered.getRGB(x, y));
                }
            }
            return native;
        }

        fun nativeToBuffered(native : NativeImage) : BufferedImage {
            val buffered = BufferedImage(native.width, native.height, IconCache.FORMAT);
            for (y in 0..<native.height) {
                for (x in 0..<native.width) {
                    buffered.setRGB(x, y, native.getColor(x, y));
                }
            }
            return buffered;
        }

        fun imageToBuffered(image : Image) : BufferedImage {
            if (image is BufferedImage) { return image; }
            val buffered = BufferedImage(image.getWidth(null), image.getHeight(null), IconCache.FORMAT);
            val g = buffered.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            return buffered;
        }

        fun scaleBuffered(buffered : Image, w : Int, h : Int) : Image {
            return buffered.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        }

        fun cropBuffered(buffered : BufferedImage, x : Int, y : Int, w : Int, h : Int) : BufferedImage {
            return buffered.getSubimage(x, y, w, h);
        }

        fun lcm(a : Int, b : Int): Int {
            val l = if (a > b) { a } else { b };
            val max = a * b;
            var lcm = l;
            while (lcm <= max) {
                if (max % a == 0 && max % b == 0) {
                    return lcm;
                }
                lcm += l;
            }
            return max;
        }

        fun correctSingleFrameImage(buffered : BufferedImage) : BufferedImage {
            var image = buffered;
            if (buffered.width > IconTransporter.MAX_SIZE) {
                val height = (buffered.height * IconTransporter.MAX_SIZE) / buffered.width;
                image = this.imageToBuffered(this.scaleBuffered(buffered,
                    IconTransporter.MAX_SIZE, height
                ));
            }
            if (image.width != image.height) {
                val maxDim = max(image.width, image.height);
                val final = BufferedImage(maxDim, maxDim, IconCache.FORMAT);
                val x = final.width  / 2 - image.width  / 2;
                val y = final.height / 2 - image.height / 2;
                val g = buffered.createGraphics();
                g.drawImage(image, x, y, null);
                g.dispose();
                return final;
            }
            return image;
        }

    }

    abstract val name : String;

    abstract fun addArguments(root : LiteralArgumentBuilder<S>);

}
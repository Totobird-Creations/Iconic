package net.totobirdcreations.iconic.generator

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.totobirdcreations.iconic.IconCache
import net.totobirdcreations.iconic.IconTransporter
import java.io.ByteArrayInputStream
import java.net.URI
import javax.imageio.ImageIO


object UrlGenerator : IconGenerator() {

    override val name : String = "url";

    override fun addArguments(root : LiteralArgumentBuilder<S>) {
        root.then(
            argument("name", StringArgumentType.word()).then(
                argument("imageUrl", StringArgumentType.greedyString())
                .executes{ ctx -> this.generateUrl(
                    StringArgumentType.getString(ctx, "name"),
                    StringArgumentType.getString(ctx, "imageUrl")
                ); 0 }
            )
        );
    }

    private fun generateUrl(name : String, target : String) {
        val data = IconTransporter.downloadIcon(URI(target));
        if (data.isFailure) {
            throwError("Failed to download icon for `${name}`: ${data.exceptionOrNull()!!.message}");
        }
        val icon = ImageIO.read(ByteArrayInputStream(data.getOrThrow()))
            ?: throwError("Failed to download icon for `${name}`: Could not create image.");
        saveIcon(Util.correctSingleFrameImage(icon), name.lowercase());
    }

}
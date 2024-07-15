package net.totobirdcreations.iconic.generator

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.minecraft.client.MinecraftClient
import net.minecraft.util.JsonHelper
import net.totobirdcreations.iconic.IconTransporter
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO


object PlayerSkinGenerator : IconGenerator() {

    override val name : String = "player";

    override fun addArguments(root : LiteralArgumentBuilder<S>) {
        root.then(
            argument("player", StringArgumentType.word())
            .suggests{ _, b -> CompletableFuture.supplyAsync{ ->
                val players = MinecraftClient.getInstance().networkHandler?.playerList;
                if (players != null) { for (player in players) {
                    b.suggest(player.profile.name);
                } }
                b.build()
            } }
            .executes{ ctx -> this.generatePlayerSkin(StringArgumentType.getString(ctx, "player")); 0 }
        );
    }

    private fun generatePlayerSkin(target : String) {
        var uuid : UUID;
        var name : String;
        try {
            uuid = UUID.fromString(target);
            name = try { this.uuidToName(uuid) } catch (_ : Exception) { uuid.toString() };
        } catch (_ : Exception) {
            name = target;
            uuid = this.nameToUuid(name);
        }
        val data = IconTransporter.downloadIcon(URI("https://visage.surgeplay.com/bust/${IconTransporter.MAX_SIZE}/${uuid.toString().replace("-", "")}.png"));
        if (data.isFailure) {
            throwError("Failed to download player skin for `${name}`: ${data.exceptionOrNull()!!.message}");
        }
        val icon = ImageIO.read(ByteArrayInputStream(data.getOrThrow()))
            ?: throwError("Failed to download player skin for `${name}`: Could not create image.");
        saveIcon(icon, name.lowercase());
    }

    private fun uuidToName(uuid : UUID) : String {
        val pl = MinecraftClient.getInstance().networkHandler?.getPlayerListEntry(uuid);
        if (pl != null) { return pl.profile.name; }
        val url = URI("https://api.mojang.com/user/profile/${uuid.toString().replace("-", "")}").toURL();
        val conn = url.openConnection() as HttpURLConnection;
        conn.requestMethod = "GET";
        conn.doInput = true;
        val stream = conn.inputStream;
        val json = stream.readAllBytes().decodeToString();
        stream.close();
        conn.disconnect();
        return JsonHelper.deserialize(json).get("name").asString;
    }
    private fun nameToUuid(name : String) : UUID {
        val pl = MinecraftClient.getInstance().networkHandler?.getPlayerListEntry(name);
        if (pl != null) { return pl.profile.id; }
        val url = URI("https://api.mojang.com/users/profiles/minecraft/${name}").toURL();
        val conn = url.openConnection() as HttpURLConnection;
        conn.requestMethod = "GET";
        conn.doInput = true;
        val stream = conn.inputStream;
        val json = stream.readAllBytes().decodeToString();
        stream.close();
        conn.disconnect();
        val str = JsonHelper.deserialize(json).get("id").asString;
        return UUID(str.substring(0, 16).toULong(16).toLong(), str.substring(16, 32).toULong(16).toLong());
    }

}
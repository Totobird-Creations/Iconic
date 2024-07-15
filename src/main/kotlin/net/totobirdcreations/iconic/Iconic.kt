package net.totobirdcreations.iconic

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.totobirdcreations.iconic.generator.IconGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import kotlin.io.path.Path

object Iconic : ClientModInitializer {
	const val ID = "iconic";
	@JvmStatic
    val LOGGER : Logger = LoggerFactory.getLogger(ID)

	override fun onInitializeClient() {
		if (FabricLoader.getInstance().environmentType == EnvType.SERVER) { throw UnsupportedOperationException("Iconic is not supported on dedicated servers. Install it on a client instead."); }

		LOGGER.info("Obligatory init message.");

		if (! IconCache.LOCAL_ICONS_PATH.isDirectory) {
			IconCache.LOCAL_ICONS_PATH.mkdirs();
		}
		// Move all default icons to icon directory.
		val provided = BufferedReader(InputStreamReader(this::class.java.getResourceAsStream("/assets/${ID}/${ID}/default/default.txt")!!));
		while (true) {
			val name   = "${provided.readLine() ?: break}.png";
			val stream = this::class.java.getResourceAsStream("/assets/${ID}/${ID}/default/${name}") ?: continue;
			val path   = IconCache.LOCAL_ICONS_PATH.resolve(name);
			if (! path.exists()) { Files.copy(stream, path.toPath()); }
		}

		IconGenerator.registerCommand();

		ClientPlayConnectionEvents.INIT.register{ _, _ -> IconCache.openThreads(); }
		ClientPlayConnectionEvents.DISCONNECT.register{ _, _ -> IconCache.closeThreads(); IconCache.invalidate(); }

		ClientSendMessageEvents.ALLOW_CHAT.register{ message -> ChatScanner.interceptOutgoingMessage(message){ msg ->
			MinecraftClient.getInstance().player?.networkHandler?.sendChatMessage(msg);
		} };
		ClientSendMessageEvents.ALLOW_COMMAND.register{ message ->
			if (ChatScanner.isInterceptableCommand(message)) {
				ChatScanner.interceptOutgoingMessage(message){ msg ->
					MinecraftClient.getInstance().player?.networkHandler?.sendChatCommand(msg);
				}
			} else { true }
		};
	}
}
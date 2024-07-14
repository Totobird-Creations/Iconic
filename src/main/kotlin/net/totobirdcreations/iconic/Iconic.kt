package net.totobirdcreations.iconic

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import kotlin.io.path.Path

object Iconic : ClientModInitializer {
	const val ID = "iconic";
	@JvmStatic
    val LOGGER = LoggerFactory.getLogger(ID)

	override fun onInitializeClient() {
		if (FabricLoader.getInstance().environmentType == EnvType.SERVER) { throw UnsupportedOperationException("Iconic is not supported on dedicated servers. Install it on a client instead."); }

		LOGGER.info("Obligatory init message.");

		// Move all default icons to icon directory.
		if (! IconCache.LOCAL_ICONS_PATH.isDirectory) {
			IconCache.LOCAL_ICONS_PATH.mkdirs();
			val provided = BufferedReader(InputStreamReader(this::class.java.getResourceAsStream("/assets/${ID}/${ID}/default/default.txt")!!));
			while (true) {
				val name   = "${provided.readLine() ?: break}.png";
				val stream = this::class.java.getResourceAsStream("/assets/${ID}/${ID}/default/${name}") ?: continue;
				Files.copy(stream, IconCache.LOCAL_ICONS_PATH.resolve(name).toPath());
			}
		}

		ClientPlayConnectionEvents.INIT.register{ _, _ -> IconCache.openThreads(); }
		ClientPlayConnectionEvents.DISCONNECT.register{ _, _ -> IconCache.closeThreads(); IconCache.invalidate(); }

		ClientSendMessageEvents.MODIFY_CHAT    .register{ message -> ChatScanner.replaceMessageIcons(message) };
		ClientSendMessageEvents.MODIFY_COMMAND .register{ message -> ChatScanner.replaceMessageIcons(message) };
	}
}
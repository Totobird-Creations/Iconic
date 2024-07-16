package net.totobirdcreations.iconic

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.totobirdcreations.iconic.figura.FiguraEmojisAccessor


object ChatScanner {

    const val OUTGOING_PREFIX : String = ":";
    const val OUTGOING_SUFFIX : String = ":";

    private val VALID_FILE_NAME_PATTERN : Regex = Regex("^[A-Za-z0-9_-]+\\.png$");

    @JvmStatic
    fun findSuggestionPrefix(message : String) : Pair<String, Int>? {
        if (message.startsWith("/") && (! this.isInterceptableCommand(message.substring(1)))) { return null; }
        var inIcon      = false;
        val currentIcon = StringBuilder();
        var i = 0;
        var m = 0;
        var escape = false;
        while (i < message.length) {
            val ch = message[i];
            if (! escape && ch == '\\') { escape = true; }
            else if (escape) { currentIcon.append(ch); escape = false; }
            else if (! inIcon) { if (message.substring(i).startsWith(OUTGOING_PREFIX)) {
                currentIcon.clear(); m = 0;
                inIcon = true;
                i += OUTGOING_PREFIX.length;
                continue;
            } } else {
                if (currentIcon.isEmpty() && ch == '#') { currentIcon.append(ch); }
                else {
                    val remaining = message.substring(i);
                    if (remaining.length < OUTGOING_SUFFIX.length
                        && remaining == OUTGOING_SUFFIX.substring(0, remaining.length)
                    ) { m = remaining.length; break; }
                    else if (remaining.startsWith(OUTGOING_SUFFIX)) {
                        i += OUTGOING_SUFFIX.length;
                        inIcon = false;
                    } else {
                        currentIcon.append(ch);
                    }
                }
            }
            i += 1;
        }
        return if (inIcon) { return Pair(currentIcon.toString(), currentIcon.length + m) } else { null };
    }
    @JvmStatic
    fun getSuggestions(prefix : String, external : Boolean) : Set<String> {
        val files = mutableSetOf<String>();
        for (name in IconCache.getDefaultIconIconIds()) { if (prefix.isEmpty() || name.contains(prefix)) {
            files.add(name);
        } }
        files.addAll(IconCache.LOCAL_ICONS_PATH.listFiles{ file ->
            if (! (file.isFile
                        && VALID_FILE_NAME_PATTERN.matches(file.name)
                        && (prefix.isEmpty() || file.nameWithoutExtension.contains(prefix))
            )) { return@listFiles false; }
            val image = IconCache.loadLocalIcon(file);
            return@listFiles image != null;
        }?.map{ file -> file.nameWithoutExtension }
            ?: listOf()
        );
        if (external) {
            files.addAll(FiguraEmojisAccessor.getEmojiNames()
                .filter{ iconName -> prefix.isEmpty() || iconName.contains(prefix) }
            );
        }
        return files;
    }

    private var alreadyIntercepted : Boolean = false;
    fun interceptOutgoingMessage(message : String, callback : (String) -> Unit) : Boolean {
        if (! this.alreadyIntercepted) {
            Thread{ ->
                val msg = this.replaceMessageIcons(message) ?: return@Thread;
                this.alreadyIntercepted = true;
                callback(msg);
                this.alreadyIntercepted = false;
            }.start();
            return false;
        }
        return true;
    }

    fun isInterceptableCommand(message : String) : Boolean {
        return     message.startsWith("me ")
                || message.startsWith("msg ")
                || message.startsWith("teammsg ")
                || message.startsWith("tell ")
                || message.startsWith("tellraw ")
                || message.startsWith("tm ")
                || message.startsWith("w ")
                || message.startsWith("reply ")
                || message.startsWith("r ")
                || message.startsWith("last ")
                || message.startsWith("l ");
    }

    /*private fun replaceMessageIcons(message : String) : String? {
        val matches = OUTGOING_ICON_PATTERN.findAll(message).toList();
        val iconNames = matches.map{ match -> match.groups[1]!!.value }.toSet();
        val threads      = Array<Thread?>(iconNames.size){ _ -> null };
        val transportIds = mutableMapOf<String, String>();
        for ((i, iconName) in iconNames.withIndex()) {
            val thread = Thread{ -> transportIds[iconName] = IconCache.loadCacheTransportLocalIcon(iconName) ?: return@Thread; };
            threads[i] = thread; thread.start();
        }
        for (thread in threads) { thread!!.join(); }

        var msg = message;
        for (match in matches) {
            val name        = match.groups[1]!!.value;
            val transportId = transportIds[name] ?: continue;
            val msg1 = msg.replaceRange((match.range.first - (message.length - msg.length))..(match.range.last - (message.length - msg.length)), "<:${name}:${transportId}:>");
            msg = msg1;
        }
        if (msg.length > 256) {
            MinecraftClient.getInstance().player?.sendMessage(Text.literal("Resulting message is too long. Try removing some icons.").formatted(Formatting.GOLD));
            return null;
        }
        return msg;
    }*/
    private fun replaceMessageIcons(message : String) : String? {
        val iconNames = mutableMapOf<String, Pair<String?, MutableList<Int>>>();

        // Find icon locations.
        var msg = StringBuilder();
        var inIcon      = false;
        val currentIcon = StringBuilder();
        var i = 0;
        var escape = false;
        while (i < message.length) {
            val ch = message[i];
            if (! escape && ch == '\\') { escape = true; }
            else if (escape) {
                if (ch == ':' && FabricLoader.getInstance().isModLoaded("figura")) {
                    currentIcon.append('\\');
                }
                currentIcon.append(ch); escape = false;
            }
            else if (! inIcon) {
                if (message.substring(i).startsWith(OUTGOING_PREFIX)) {
                    msg.append(currentIcon.toString());
                    currentIcon.clear();
                    inIcon = true;
                    i += OUTGOING_PREFIX.length;
                    continue;
                } else {
                    currentIcon.append(ch);
                }
            } else {
                val remaining = message.substring(i);
                if (remaining.startsWith(OUTGOING_SUFFIX)) {
                    iconNames.getOrPut(currentIcon.toString()){ -> Pair(null, mutableListOf()) }.second.add(msg.length);
                    currentIcon.clear();
                    inIcon = false;
                    i += OUTGOING_SUFFIX.length;
                    continue;
                }
                currentIcon.append(ch);
            }
            i += 1;
        }
        if (inIcon) { msg.append(':'); }
        msg.append(currentIcon.toString());


        // Insert Iconic icons.
        val replaceEntries = iconNames.entries;
        val threads        = Array<Thread?>(iconNames.size){ _ -> null };
        for ((i, e) in replaceEntries.withIndex()) {
            val (name, v) = e;
            val (_, indices) = v;
            val thread = Thread{ -> iconNames[name] = Pair(IconCache.loadCacheTransportLocalIcon(name) ?: return@Thread, indices); };
            threads[i] = thread; thread.start();
        }
        for (thread in threads) { thread!!.join(); }

        val msg0 = "${msg}";
        for ((name, v) in replaceEntries) {
            val (transportId, indices) = v;
            for (index in indices) {
                msg.insert(index - (msg0.length - msg.length),
                    if (transportId == null) { ":${name}:" }
                    else { "<:${name}:${transportId}:>" }
                );
            }
        }

        if (msg.length > 256) {
            MinecraftClient.getInstance().player?.sendMessage(Text.literal("Resulting message is too long. Try removing some icons.").formatted(Formatting.GOLD));
            return null;
        }
        return msg.toString();
    }

    private val INCOMING_ICON_PATTERN : Regex = Regex("<:(#?[A-Za-z0-9_-]+:[0-9]+):>");
    @JvmStatic
    fun splitIcons(message : String) : List<String> {
        var len = 0;
        val components = mutableListOf<String>();
        for (match in INCOMING_ICON_PATTERN.findAll(message)) {
            components.add(message.substring(len, match.range.first));
            components.add(match.groups[1]!!.value);
            len = match.range.last + 1;
        }
        if (len < message.length) {
            components.add(message.substring(len));
        }
        return components;
    }

}
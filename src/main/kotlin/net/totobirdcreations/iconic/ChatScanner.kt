package net.totobirdcreations.iconic

import net.totobirdcreations.iconic.figura.FiguraEmojisAccessor


object ChatScanner {

    const val OUTGOING_PREFIX : String = ":";
    const val OUTGOING_SUFFIX : String = ":";

    private val VALID_FILE_NAME_PATTERN : Regex = Regex("^[A-Za-z0-9_-]+\\.png$");
    private val VALID_ICON_CHARACTER_PREDICATE  = { ch : Char -> (ch in 'A'..'Z') || (ch in 'a'..'z') || (ch in '0'..'9') || (ch == '_') || (ch == '-') };

    private val OUTGOING_ICON_PATTERN : Regex = Regex("${OUTGOING_PREFIX.map{ ch -> "\\${ch}" }}(#?[A-Za-z0-9_-]+)${OUTGOING_SUFFIX.map{ ch -> "\\${ch}" }}");

    @JvmStatic
    fun findSuggestionPrefix(message : String) : Pair<String, Int>? {
        if (message.startsWith("/") && (! this.isInterceptableCommand(message.substring(1)))) { return null; }
        var inIcon      = false;
        val currentIcon = StringBuilder();
        var i = 0;
        var m = 0;
        while (i < message.length) {
            val ch = message[i];
            if (! inIcon) { if (message.substring(i).startsWith(OUTGOING_PREFIX)) {
                currentIcon.clear(); m = 0;
                inIcon = true;
                i += OUTGOING_PREFIX.length;
                continue;
            } } else {
                if (currentIcon.isEmpty() && ch == '#') { currentIcon.append(ch); }
                else if (ch != ':') {
                    currentIcon.append(ch);
                }
                else {
                    val remaining = message.substring(i);
                    if (remaining.length < OUTGOING_SUFFIX.length
                        && remaining == OUTGOING_SUFFIX.substring(0, remaining.length)
                    ) { m = remaining.length; break; }
                    inIcon = false;
                }
            }
            i += 1;
        }
        return if (inIcon) { return Pair(currentIcon.toString(), currentIcon.length + m) } else { null };
    }
    @JvmStatic
    fun getSuggestions(prefix : String) : Set<String> {
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
            return@listFiles (image != null && IconCache.validateIcon(image.width).isSuccess);
        }?.map{ file -> file.nameWithoutExtension }
            ?: listOf()
        );
        files.addAll(FiguraEmojisAccessor.getEmojiNames()
            .filter{ iconName -> prefix.isEmpty() || iconName.contains(prefix) }
        );
        return files;
    }

    private var alreadyIntercepted : Boolean = false;
    fun interceptOutgoingMessage(message : String, callback : (String) -> Unit) : Boolean {
        if ((! (this.alreadyIntercepted)) && OUTGOING_ICON_PATTERN.containsMatchIn(message)) {
            Thread{ ->
                val msg = this.replaceMessageIcons(message);
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

    private fun replaceMessageIcons(message : String) : String {
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
            if (msg1.length > 256) { break; }
            msg = msg1;
        }
        return msg;
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
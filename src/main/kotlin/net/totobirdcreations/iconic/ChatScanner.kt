package net.totobirdcreations.iconic

object ChatScanner {

    private const val OUTGOING_PREFIX : String = "<:";
    private const val OUTGOING_SUFFIX : String = ":>";

    // Remove last char and add `?` after each char.
    private val POTENTIAL_OUTGOING_SUFFIX : String = OUTGOING_SUFFIX.substring(0, OUTGOING_SUFFIX.length - 1).map{ ch -> "${ch}?" }.joinToString();

    private val VALID_FILE_NAME : Regex = Regex("^[A-Za-z0-9_-]+\\.png$");

    private val SUGGESTION_TRIGGER_PATTERN : Regex = Regex("^.*${OUTGOING_PREFIX}((?:(?!${OUTGOING_SUFFIX})[A-Za-z0-9_-])*)${POTENTIAL_OUTGOING_SUFFIX}$");
    @JvmStatic
    fun findSuggestionPrefix(message : String) : String? {
        return SUGGESTION_TRIGGER_PATTERN.find(message)?.groups?.get(1)?.value;
    }
    @JvmStatic
    fun getSuggestions(prefix : String) : List<String> {
        return IconCache.LOCAL_ICONS_PATH.listFiles{ file ->
            if (! (file.isFile
                        && VALID_FILE_NAME.matches(file.name)
                        && (prefix.isEmpty() || file.nameWithoutExtension.contains(prefix))
            )) { return@listFiles false; }
            val image = IconCache.loadLocalIcon(file);
            return@listFiles (image != null && IconCache.validateIcon(image).isSuccess);
        }?.map{ file -> file.nameWithoutExtension }
            ?: listOf();
    }


    private val OUTGOING_ICON_PATTERN : Regex = Regex("${OUTGOING_PREFIX}([A-Za-z0-9_-]+)${OUTGOING_SUFFIX}");
    fun replaceMessageIcons(message : String) : String {
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

    private val INCOMING_ICON_PATTERN : Regex = Regex("<:([A-Za-z0-9_-]+:[0-9]+):>");
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
package net.totobirdcreations.iconic

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.FontStorage
import net.minecraft.client.texture.NativeImage
import net.totobirdcreations.iconic.generator.IconGenerator
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream


private typealias RemoteIcon = Quadruple<
        IconFontStorage?,
        IconRenderer.IconGlyphRenderer,
        IconRenderer.IconGlyph,
        NativeImage
>;


object IconCache {

    const val FORMAT : Int = BufferedImage.TYPE_4BYTE_ABGR;


    ///////////////////
    //     LOCAL     //
    ///////////////////

    /**
     * How long before assuming that the icon needs to be reuploaded.
     */
    private const val REMOTE_EXPIRE_TIME : Long = 3300000L; // 55 minutes. Remote discards files after 60 minutes.

    /**
     * The path to the icon directory.
     */
    @JvmStatic
    val LOCAL_ICONS_PATH : File = File(MinecraftClient.getInstance().runDirectory, Iconic.ID);

    /**
     * Displayed while the icon is being downloaded.
     */
    private var loadingIcon : RemoteIcon? = null;
    private fun getLoadingIcon() : RemoteIcon {
        if (this.loadingIcon == null) {
            this.loadingIcon = this.loadGlyph(this.loadInternalIcon("loading")!!)
        }
        return this.loadingIcon!!;
    }
    /**
     * Displayed if the icon failed to download.
     */
    private var errorIcon : RemoteIcon? = null;
    private fun getErrorIcon() : RemoteIcon {
        if (this.errorIcon == null) {
            this.errorIcon = this.loadGlyph(this.loadInternalIcon("error")!!)
        }
        return this.errorIcon!!;
    }
    /**
     * Displayed behind the texture in the expand screen.
     */
    private var gridIcon : RemoteIcon? = null;
    fun getGridIcon() : RemoteIcon {
        if (this.gridIcon == null) {
            this.gridIcon = this.loadGlyph(this.loadInternalIcon("grid")!!)
        }
        return this.gridIcon!!;
    }

    //                                                  ,-Transport ID.
    //                                                  |       ,-Icon file load time.
    //                                   v-File name.   v       v     v-Remote expiration time.
    private val localIcons  : MutableMap<String, Triple<String, Long, Long>> = mutableMapOf();
    private val remoteIcons : MutableMap<String, RemoteIcon?>                = mutableMapOf();
    //                                   ^-Transport ID.

    /**
     * Get a transport ID for an icon from the icons directory by name, caching, reloading, uploading, etc as necessary.
     */
    fun loadCacheTransportLocalIcon(name : String) : String? { return this.loadCacheTransportLocalIcon(LOCAL_ICONS_PATH.resolve("${name}.png")); }
    /**
     * Get a transport ID for an icon from the icons directory by file, caching, reloading, uploading, etc as necessary.
     */
    private fun loadCacheTransportLocalIcon(file : File) : String? {
        val cachedIcon = this.localIcons[file.path];
        var glyph : RemoteIcon? = null;
        if (cachedIcon != null) {
            if (file.lastModified() < cachedIcon.second) { // File has not been modified.
                if (System.currentTimeMillis() < cachedIcon.third) { // Remote has not expired.
                    return cachedIcon.first;
                } else {
                    Iconic.LOGGER.info("Icon `${file.nameWithoutExtension}` timed out. Reuploading...");
                }
                glyph = this.remoteIcons[cachedIcon.first]!!;
            } else {
                Iconic.LOGGER.info("Icon `${file.nameWithoutExtension}` was modified. Reuploading...");
            }
        }
        // If the icon isn't cached or has been modified, get it.
        if (glyph == null) {
            Iconic.LOGGER.info("Icon `${file.nameWithoutExtension}` has not been cached. Reloading...");
            val icon = this.loadLocalIcon(file);
            if (icon == null) {
                Iconic.LOGGER.error("Failed to load icon `${file.nameWithoutExtension}`: Could not create image.");
                return null;
            }
            val error = this.validateIcon(icon.width).exceptionOrNull();
            if (error != null) {
                Iconic.LOGGER.error("Failed to load icon `${file.nameWithoutExtension}`: ${error.message}");
                return null;
            }
            glyph = this.loadGlyph(icon);
        }
        val transportId = IconTransporter.uploadIcon(file.name, file.inputStream().readAllBytes()).getOrElse{ e ->
            Iconic.LOGGER.error("Icon `${file.nameWithoutExtension}` failed to upload: ${e.message}");
            return null;
        };
        Iconic.LOGGER.info("Icon `${file.nameWithoutExtension}` uploaded. Transport ID obtained: `${transportId}`.");
        this.localIcons[file.path] = Triple(transportId, System.currentTimeMillis(), System.currentTimeMillis() + REMOTE_EXPIRE_TIME);
        this.remoteIcons[transportId] = glyph; // Comment out to test download system.
        return transportId;
    }
    /**
     * Make sure that the icon is following all rules.
     */
    fun validateIcon(width : Int) : Result<Unit> {
        if (width > IconTransporter.MAX_SIZE) { return Result.failure(Exception("Image width may not exceed ${IconTransporter.MAX_SIZE}.")); }
        return Result.success(Unit);
    }

    /**
     * Load an icon from resources in directory `assets/${ID}/${ID}`.
     */
    private fun loadInternalIcon(name : String) : NativeImage? {
        return this.loadIcon(this.getInternalIconStream(name)!!);
    }
    fun getInternalIconStream(name : String) : InputStream? {
        return this::class.java.getResourceAsStream("/assets/${Iconic.ID}/${Iconic.ID}/${name}.png");
    }

    /**
     * Load an icon from the icons directory by name.
     */
    fun loadLocalIcon(name : String) : NativeImage? { return this.loadLocalIcon(LOCAL_ICONS_PATH.resolve(name)) }
    /**
     * Load an icon from the icons directory by file.
     */
    fun loadLocalIcon(file : File) : NativeImage? { return try { this.loadIcon(file.inputStream()) } catch (e : Exception) { null }; }
    /**
     * Load an icon from a bytearray.
     */
    private fun loadIcon(stream : ByteArray) : NativeImage? { return this.loadIcon(ByteArrayInputStream(stream)); }
    /**
     * Load an icon from a stream.
     */
    private fun loadIcon(stream : InputStream) : NativeImage? { return try { NativeImage.read(stream) } catch (e : Exception) { null }; }
    /**
     * Get glyph information from an icon.
     */
    private fun loadGlyph(image : NativeImage) : RemoteIcon {
        val renderer = IconRenderer.IconGlyphRenderer((image.height.toFloat() / image.width.toFloat()).toInt());
        RenderSystem.recordRenderCall{
            val glID : Int = TextureUtil.generateTextureId();
            renderer.iconGlID = glID;
            TextureUtil.prepareImage(glID, image.width, image.height);
            image.upload(0, 0, 0, true);
        };
        val glyph = IconRenderer.IconGlyph(renderer);
        return RemoteIcon(null, renderer, glyph, image);
    }



    ////////////////////
    //     REMOTE     //
    ////////////////////

    /**
     * Number of threads used to simultaneously download icons.
     */
    private const val DOWNLOADER_THREAD_COUNT : Int = 4;

    /**
     * Icons queued for downloading.
     */
    private val queuedIconDownloads : HashSet<String> = hashSetOf();
    //                                     ^-Transport ID.
    /**
     * Icons queued for or currently downloading.
     */
    private val activeIconDownloads : HashSet<String> = hashSetOf();
    //                                        ^-Transport ID.

    /**
     * Get an icon from cache, or queue for downloading if necessary.
     */
    private fun getCachedRemoteIconOrDownload(transportId : String) : RemoteIcon {
        if (! this.remoteIcons.containsKey(transportId)) {
            if (! this.activeIconDownloads.contains(transportId)) {
                Iconic.LOGGER.info("Queued icon `${transportId}` for downloading...");
                this.activeIconDownloads.add(transportId);
                this.queuedIconDownloads.add(transportId);
            }
            return this.getLoadingIcon();
        }
        return this.remoteIcons[transportId] ?: this.getErrorIcon();
    }
    /**
     * Get an icon from the cache, returning the error icon if it is not cached.
     */
    fun getCachedRemoteIcon(transportId : String) : RemoteIcon {
        return this.remoteIcons[transportId] ?: this.getErrorIcon();
    }
    /**
     * Gets an icon's font storage from cache.
     */
    @JvmStatic
    fun getRemoteFontStorage(transportId : String, defaultFont : FontStorage) : FontStorage {
        val cri = this.getCachedRemoteIconOrDownload(transportId);
        if (cri.first != null) { return cri.first; } else {
            val ifs = IconFontStorage(cri.second, cri.third, defaultFont);
            this.remoteIcons[transportId] = RemoteIcon(ifs, cri.second, cri.third, cri.fourth);
            return ifs;
        }
    }

    /**
     * Copies a remote icon to the local icon directory.
     */
    fun copyRemoteIconToLocal(transportId : String, name : String) : File? {
        val icon = this.getCachedRemoteIcon(transportId);
        val file = IconGenerator.findSaveLocation(name);
        try {
            GlStateManager._bindTexture(icon.second.iconGlID!!);
            val image = NativeImage(icon.fourth.width, icon.fourth.height, false);
            image.loadFromTextureImage(0, false);
            image.writeTo(file);
            image.close();
        } catch (e : Exception) {
            Iconic.LOGGER.error("Failed to copy icon `${name}`: ${e.message}.");
            return null;
        }
        return file;
    }

    private val downloaderThreads : MutableList<Thread> = mutableListOf();
    private var downloadersOpen   : Boolean             = false;
    fun openThreads() {
        this.downloadersOpen = true;
        for (i in 0..<DOWNLOADER_THREAD_COUNT) {
            val thread = Thread(this::downloaderThread);
            this.downloaderThreads.add(thread);
            thread.start();
        }
    }
    fun closeThreads() {
        this.downloadersOpen = false;
        for (thread in this.downloaderThreads) {
            try {
                thread.interrupt();
                thread.join();
            } catch (_ : Exception) { }
        }
        this.downloaderThreads.clear();
        this.queuedIconDownloads.clear();
    }
    private fun downloaderThread() { try {
        while (this.downloadersOpen) {
            var transportIdNull : String? = null;
            synchronized(this.queuedIconDownloads){ ->
                if (this.queuedIconDownloads.isNotEmpty()) {
                    transportIdNull = this.queuedIconDownloads.first();
                    this.queuedIconDownloads.remove(transportIdNull);
                }
            }
            if (transportIdNull == null) {
                Thread.sleep(100);
                continue;
            }
            val transportId = transportIdNull!!;

            val dataResult = IconTransporter.downloadIcon(transportId);
            if (dataResult.isFailure) { this.downloadError(transportId, dataResult.exceptionOrNull()!!.message); continue; }
            val data = dataResult.getOrThrow();
            val icon = this.loadIcon(data);
            if (icon == null) { this.downloadError(transportId, "Invalid or corrupt image data"); continue; }
            val error = this.validateIcon(icon.width).exceptionOrNull();
            if (error != null) { this.downloadError(transportId, error.message); continue; }

            Iconic.LOGGER.info("Icon `${transportId}` downloaded.");
            this.remoteIcons[transportId] = this.loadGlyph(icon);
            this.activeIconDownloads.remove(transportId);
        }
    } catch (_ : Exception) {} }
    private fun downloadError(transportId : String, message : String?) {
        this.remoteIcons[transportId] = null;
        this.activeIconDownloads.remove(transportId);
        Iconic.LOGGER.error("Failed to download icon `${transportId}`: ${message ?: "No message given."}");
    }



    fun invalidate() {
        this.localIcons.clear();
        for ((_, remoteIcon) in this.remoteIcons) {
            if (remoteIcon != null) {
                val glID = remoteIcon.second.iconGlID ?: continue;
                TextureUtil.releaseTextureId(glID);
                remoteIcon.fourth.close();
            }
        }
        this.remoteIcons.clear();
    }


}
package net.totobirdcreations.iconic

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.PressableTextWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.min


class IconViewScreen(
    private val transportId : String,
    private val name        : String,
    private val namespace   : IconRenderer.IconNamespace
) : Screen(Text.literal("View Icon")) {
    companion object {
        private val DOWNLOAD_TEXT = Text.literal("ᴅᴏᴡɴʟᴏᴀᴅ").styled{ s -> s.withColor(Formatting.YELLOW) };
    }
    private val lastScreen : Screen? = MinecraftClient.getInstance().currentScreen;

    private var nameTextWidget        : TextWidget? = null;
    private var transportIdTextWidget : TextWidget? = null;

    private var downloadButton      : PressableTextWidget? = null;
    private var namespaceTextWidget : TextWidget?          = null;
    private val downloadable        : Boolean              = this.namespace != IconRenderer.IconNamespace.Iconic;

    private val gridRenderer = IconCache.getGridIcon().second;
    private val iconRenderer = IconCache.getCachedRemoteIcon(this.transportId).second;

    private var imageMinX : Float = 0.0f;
    private var imageMinY : Float = 0.0f;
    private var imageSize : Float = 0.0f;

    override fun init() {
        if (this.nameTextWidget == null) {
            this.nameTextWidget = TextWidget(
                Text.literal(this.name).styled{ s -> s.withBold(true).withUnderline(true).withColor(Formatting.WHITE) },
                this.textRenderer
            )
            this.transportIdTextWidget = TextWidget(
                Text.literal(this.transportId).styled{ s -> s.withColor(Formatting.DARK_GRAY) },
                this.textRenderer
            );
            this.downloadButton = PressableTextWidget(
                0, 0, 0, 0, DOWNLOAD_TEXT,
                { _ ->
                    val path = IconCache.copyRemoteIconToLocal(this.transportId, this.name);
                    if (path == null) {
                        this.client?.player?.sendMessage(Text.literal("Failed to clone icon `${this.name}`.").styled{ s -> s.withColor(Formatting.RED) });
                    } else {
                        this.client?.player?.sendMessage(Text.literal("Cloned icon `${this.name}` as `${path.nameWithoutExtension}`.").styled{ s -> s.withColor(Formatting.YELLOW) });
                    }
                    this.close();
                },
                this.textRenderer
            );
            this.namespaceTextWidget = TextWidget(
                Text.literal(this.namespace.viewText).styled{ s -> s.withColor(this.namespace.colour) },
                this.textRenderer
            );
        }

        val minDim = min(this.width, this.height);
        this.imageMinX = this.width  / 2.0f - minDim / 4.0f;
        this.imageMinY = this.height / 2.0f - minDim / 4.0f;
        this.imageSize = minDim / 2.0f;

        this.nameTextWidget        !!.setPosition(4, this.height - (2 + this.textRenderer.fontHeight) * 2 );
        this.transportIdTextWidget !!.setPosition(4, this.height - (2 + this.textRenderer.fontHeight)     );
        this.addDrawableChild(this.nameTextWidget        );
        this.addDrawableChild(this.transportIdTextWidget );

        val target = (if (this.downloadable) { this.downloadButton } else { this.namespaceTextWidget })!!;
        target.width  = this.textRenderer.getWidth(target.message);
        target.height = this.textRenderer.fontHeight;
        target.setPosition(
            this.width / 2 - target.width / 2,
            this.height / 2 + minDim / 4
        );
        this.addDrawableChild(target);
    }

    override fun render(context : DrawContext, mouseX : Int, mouseY : Int,  delta: Float) {
        super.render(context, mouseX, mouseY, delta);
        this.gridRenderer.draw(
            this.imageMinX, this.imageMinY, this.imageSize, this.imageSize,
            context.matrices.peek().positionMatrix,
            1.0f, 1.0f, 1.0f, 1.0f
        );
        this.iconRenderer.draw(
            this.imageMinX, this.imageMinY, this.imageSize, this.imageSize,
            context.matrices.peek().positionMatrix,
            1.0f, 1.0f, 1.0f, 1.0f
        );
    }


    override fun close() {
        MinecraftClient.getInstance().setScreen(this.lastScreen);
    }

}
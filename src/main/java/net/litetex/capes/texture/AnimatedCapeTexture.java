package net.litetex.capes.texture;

import net.litetex.capes.CapeProviderX;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITickableTextureObject;
import net.minecraft.client.resources.IResourceManager;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class AnimatedCapeTexture extends DynamicTexture implements ITickableTextureObject {

    private static final int DEFAULT_TICKS_PER_FRAME = 2;

    private final int[][] frames;
    private final int[] frameTicks;
    private final int frameCount;
    private final int frameW;
    private final int frameH;
    private int currentFrame = 0;
    private int tickCounter = 0;

    public AnimatedCapeTexture(BufferedImage spritesheet) {
        this(spritesheet, null);
    }

    public AnimatedCapeTexture(BufferedImage spritesheet, int[] ticksPerFrame) {
        super(spritesheet.getWidth(), Math.max(1, spritesheet.getWidth() / 2));

        this.frameW = spritesheet.getWidth();
        this.frameH = Math.max(1, spritesheet.getWidth() / 2);
        this.frameCount = Math.max(1, spritesheet.getHeight() / this.frameH);
        this.frames = new int[frameCount][];
        this.frameTicks = new int[frameCount];

        CapeProviderX.LOGGER.info(
            "[CapeProviderX] AnimatedCapeTexture: {}x{} spritesheet, {} frame(s) at {}x{}",
            spritesheet.getWidth(), spritesheet.getHeight(),
            frameCount, frameW, frameH);

        for (int i = 0; i < frameCount; i++) {
            BufferedImage frame = spritesheet.getSubimage(0, i * frameH, frameW, frameH);
            int[] pixels = new int[frameW * frameH];
            frame.getRGB(0, 0, frameW, frameH, pixels, 0, frameW);
            frames[i] = pixels;
            frameTicks[i] = ticksPerFrame != null
                && i < ticksPerFrame.length
                && ticksPerFrame[i] > 0
                ? ticksPerFrame[i]
                : DEFAULT_TICKS_PER_FRAME;
        }

        uploadFrame(0);
    }

    @Override
    public void tick() {
        if (frameCount <= 1) return;

        tickCounter++;
        if (tickCounter < frameTicks[currentFrame]) return;
        tickCounter = 0;

        currentFrame = (currentFrame + 1) % frameCount;
        uploadFrame(currentFrame);
    }

    private void uploadFrame(int index) {
        int[] data = getTextureData();
        if (data == null || data.length < frames[index].length) return;
        System.arraycopy(frames[index], 0, data, 0, frames[index].length);
        updateDynamicTexture();
    }

    @Override
    public void loadTexture(IResourceManager mgr) throws IOException {
        uploadFrame(currentFrame);
    }
}

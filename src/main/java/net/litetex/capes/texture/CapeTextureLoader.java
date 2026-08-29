package net.litetex.capes.texture;

import net.litetex.capes.CapeProviderX;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public final class CapeTextureLoader {

    private static final int  CONNECT_TIMEOUT_MS = 5000;
    private static final int  READ_TIMEOUT_MS    = 10000;
    private static final int  DEFAULT_FRAME_TICKS = 2;
    private static final long MAX_BYTES          = 10L * 1024 * 1024;

    private CapeTextureLoader() {}

    public interface TextureCallback {
        void onLoaded(ResourceLocation location);
    }

    public static void loadTexture(final String key,
                               final String urlString,
                               final String userAgent,
                               final boolean animatedEnabled,
                               final TextureCallback callback) {
    try {
        final LoadedCape loaded = loadCape(urlString, userAgent);
        if (loaded == null || loaded.image == null) return;

        CapeProviderX.LOGGER.info(
            "[CapeProviderX] Downloaded for key={}: {}x{} ({})",
            key, loaded.sourceWidth, loaded.sourceHeight, loaded.kind);

        if (loaded.normalizedChanged) {
            CapeProviderX.LOGGER.info(
                "[CapeProviderX] Normalized cape layout {}x{} -> {}x{} ({} frame(s), copied {}x{} per frame)",
                loaded.sourceWidth, loaded.sourceHeight,
                loaded.image.getWidth(), loaded.image.getHeight(),
                loaded.frameCount, loaded.sourceWidth, loaded.sourceFrameHeight);
        }

        final boolean sourceIsAnimated = loaded.frameCount > 1;

        final BufferedImage finalImage;
        final boolean animated;
        final int finalFrameCount;
        final int[] finalFrameTicks;

        if (sourceIsAnimated && !animatedEnabled) {
            int frameH = loaded.sourceFrameHeight;
            finalImage = loaded.image.getSubimage(0, 0, loaded.image.getWidth(), frameH);
            animated = false;
            finalFrameCount = 1;
            finalFrameTicks = null;
            CapeProviderX.LOGGER.info(
                "[CapeProviderX] Animated textures disabled — using first frame only ({}x{})",
                finalImage.getWidth(), finalImage.getHeight());
        } else {
            finalImage = loaded.image;
            animated = sourceIsAnimated;
            finalFrameCount = loaded.frameCount;
            finalFrameTicks = loaded.frameTicks;
        }

        CapeProviderX.LOGGER.info(
            "[CapeProviderX] Final texture: {}x{}, {} frame(s), animated={}",
            finalImage.getWidth(), finalImage.getHeight(), finalFrameCount, animated);

        Minecraft.getMinecraft().addScheduledTask(new Callable<Object>() {
            @Override
            public Object call() {
                try {
                    ResourceLocation loc = new ResourceLocation(
                        "capeproviderx",
                        "cape_" + key.replace("-", "").replace(":", ""));

                    if (animated) {
                        AnimatedCapeTexture tex =
                            new AnimatedCapeTexture(finalImage, finalFrameTicks);
                        Minecraft.getMinecraft()
                            .getTextureManager()
                            .loadTickableTexture(loc, tex);
                        CapeProviderX.LOGGER.info(
                            "[CapeProviderX] Registered animated cape: {} ({} frames, {}x{})",
                            loc, finalFrameCount,
                            finalImage.getWidth(), finalImage.getHeight());
                    } else {
                        DynamicTexture tex = new DynamicTexture(finalImage);
                        Minecraft.getMinecraft()
                            .getTextureManager()
                            .loadTexture(loc, tex);
                        CapeProviderX.LOGGER.info(
                            "[CapeProviderX] Registered static cape: {} ({}x{})",
                            loc, finalImage.getWidth(), finalImage.getHeight());
                    }

                    callback.onLoaded(loc);
                } catch (Exception e) {
                    CapeProviderX.LOGGER.warn(
                        "[CapeProviderX] Texture register failed for {}: {}",
                        key, e.getMessage());
                }
                return null;
            }
        });
    } catch (Exception e) {
        CapeProviderX.LOGGER.warn(
            "[CapeProviderX] Download failed for {}: {}",
            urlString, e.getMessage());
    }
}

public static void loadTexture(final String key,
                               final String urlString,
                               final String userAgent,
                               final TextureCallback callback) {
    loadTexture(key, urlString, userAgent, true, callback);
}

    public static void loadTexture(final String key,
                                   final String urlString,
                                   final TextureCallback callback) {
        loadTexture(key, urlString, "CapeProviderX/1.0 Forge/1.8.9", callback);
    }

    private static LoadedCape loadCape(String urlString, String userAgent) throws Exception {
        byte[] bytes = downloadBytes(urlString, userAgent);
        if (bytes == null || bytes.length == 0) return null;

        if (isGif(urlString, bytes)) {
            LoadedCape gif = decodeGif(bytes);
            if (gif != null) return gif;
        }

        BufferedImage raw = ImageIO.read(new ByteArrayInputStream(bytes));
        if (raw == null) return null;

        BufferedImage image = ensureARGB(raw);
        CapeImage normalized = normalizeCapeLayout(image);
        return new LoadedCape(
            normalized.image,
            null,
            normalized.frameCount,
            normalized.sourceWidth,
            normalized.sourceHeight,
            normalized.sourceFrameHeight,
            normalized.changed,
            "png");
    }

    private static LoadedCape decodeGif(byte[] bytes) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) return null;

        ImageReader reader = readers.next();
        try {
            ImageInputStream in =
                ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
            try {
                reader.setInput(in, false);

                int frameCount = reader.getNumImages(true);
                if (frameCount <= 0) return null;

                int[] screenSize = readGifScreenSize(reader);
                BufferedImage master = new BufferedImage(
                    screenSize[0], screenSize[1], BufferedImage.TYPE_INT_ARGB);
                List<BufferedImage> frames = new ArrayList<BufferedImage>();
                List<Integer> frameTicks = new ArrayList<Integer>();
                boolean normalizedChanged = false;
                int sourceFrameHeight = screenSize[1];

                for (int i = 0; i < frameCount; i++) {
                    BufferedImage before = copyImage(master);
                    BufferedImage frame = ensureARGB(reader.read(i));
                    FrameMeta meta = readGifFrameMeta(reader, i);

                    Graphics2D g = master.createGraphics();
                    g.drawImage(frame, meta.x, meta.y, null);
                    g.dispose();

                    CapeImage normalized = normalizeSingleFrame(master);
                    frames.add(normalized.image);
                    frameTicks.add(Integer.valueOf(delayCentisecondsToTicks(meta.delayCentiseconds)));
                    normalizedChanged = normalizedChanged || normalized.changed;
                    sourceFrameHeight = normalized.sourceFrameHeight;

                    if ("restoreToPrevious".equals(meta.disposalMethod)) {
                        master = before;
                    } else if ("restoreToBackgroundColor".equals(meta.disposalMethod)) {
                        Graphics2D clear = master.createGraphics();
                        clear.setComposite(AlphaComposite.Clear);
                        clear.fillRect(meta.x, meta.y, frame.getWidth(), frame.getHeight());
                        clear.dispose();
                    }
                }

                if (frames.isEmpty()) return null;

                BufferedImage sheet = stackFrames(frames);
                int[] ticks = new int[frameTicks.size()];
                for (int i = 0; i < frameTicks.size(); i++) {
                    ticks[i] = frameTicks.get(i).intValue();
                }

                return new LoadedCape(
                    sheet,
                    ticks,
                    frames.size(),
                    screenSize[0],
                    screenSize[1],
                    sourceFrameHeight,
                    normalizedChanged || frames.size() > 1,
                    "gif");
            } finally {
                if (in != null) in.close();
            }
        } finally {
            reader.dispose();
        }
    }

    static CapeImage normalizeCapeLayout(BufferedImage src) {
    int sourceWidth  = src.getWidth();
    int sourceHeight = src.getHeight();

    int vanillaFrameHeight = Math.max(1, sourceWidth / 2);
    if (sourceHeight > vanillaFrameHeight && sourceHeight % vanillaFrameHeight == 0) {
        int frameCount = sourceHeight / vanillaFrameHeight;
        return new CapeImage(src, frameCount, sourceWidth, sourceHeight, vanillaFrameHeight, false);
    }
    return padStaticToPowerOfTwoCanvas(src, sourceWidth, sourceHeight);
}

private static CapeImage padStaticToPowerOfTwoCanvas(BufferedImage src, int sourceWidth, int sourceHeight) {
    int canvasWidth  = 64;
    int canvasHeight = 32;
    while (canvasWidth < sourceWidth || canvasHeight < sourceHeight) {
        canvasWidth  *= 2;
        canvasHeight *= 2;
    }

    if (canvasWidth == sourceWidth && canvasHeight == sourceHeight) {
        return new CapeImage(src, 1, sourceWidth, sourceHeight, sourceHeight, false);
    }

    BufferedImage out = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.drawImage(src, 0, 0, null);
    g.dispose();

    return new CapeImage(out, 1, sourceWidth, sourceHeight, sourceHeight, true);
}

private static CapeImage normalizeSingleFrame(BufferedImage src) {
    return padStaticToPowerOfTwoCanvas(src, src.getWidth(), src.getHeight());
}

    private static BufferedImage stackFrames(List<BufferedImage> frames) {
        BufferedImage first = frames.get(0);
        int width = first.getWidth();
        int frameHeight = first.getHeight();
        BufferedImage sheet = new BufferedImage(
            width, frameHeight * frames.size(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < frames.size(); i++) {
            g.drawImage(frames.get(i), 0, i * frameHeight, null);
        }
        g.dispose();
        return sheet;
    }

    private static byte[] downloadBytes(String urlString, String userAgent) throws Exception {
        if (urlString.startsWith("data:")) {
            return readDataUriBytes(urlString);
        }

        if (urlString.startsWith("file:")) {
            InputStream is = new URL(urlString).openStream();
            try {
                return readLimited(is);
            } finally {
                is.close();
            }
        }

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setInstanceFollowRedirects(true);
        conn.connect();

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            CapeProviderX.LOGGER.info(
                "[CapeProviderX] {} returned HTTP {}", urlString, code);
            conn.disconnect();
            return null;
        }

        long len = conn.getContentLengthLong();
        if (len > MAX_BYTES) {
            CapeProviderX.LOGGER.warn(
                "[CapeProviderX] Texture too large ({} bytes) at {}", len, urlString);
            conn.disconnect();
            return null;
        }

        try {
            InputStream is = conn.getInputStream();
            try {
                return readLimited(is);
            } finally {
                is.close();
            }
        } finally {
            conn.disconnect();
        }
    }

    private static byte[] readDataUriBytes(String urlString) throws IOException {
        int comma = urlString.indexOf(',');
        if (comma < 0) return null;

        String meta = urlString.substring(5, comma).toLowerCase(Locale.ROOT);
        if (!meta.contains(";base64")) return null;

        byte[] bytes = Base64.getDecoder().decode(urlString.substring(comma + 1));
        if (bytes.length > MAX_BYTES) {
            CapeProviderX.LOGGER.warn(
                "[CapeProviderX] Embedded texture too large ({} bytes)",
                bytes.length);
            return null;
        }
        return bytes;
    }

    private static byte[] readLimited(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0L;
        int read;
        while ((read = is.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_BYTES) {
                throw new IOException("Texture too large");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static boolean isGif(String urlString, byte[] bytes) {
        if (bytes.length >= 6) {
            String magic = new String(bytes, 0, 6);
            if ("GIF87a".equals(magic) || "GIF89a".equals(magic)) return true;
        }
        return urlString.toLowerCase(Locale.ROOT).contains(".gif");
    }

    private static int[] readGifScreenSize(ImageReader reader) throws IOException {
        int width = -1;
        int height = -1;
        IIOMetadata metadata = reader.getStreamMetadata();
        if (metadata != null && metadata.getNativeMetadataFormatName() != null) {
            Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            NodeList descriptors = ((IIOMetadataNode) root)
                .getElementsByTagName("LogicalScreenDescriptor");
            if (descriptors.getLength() > 0) {
                IIOMetadataNode descriptor = (IIOMetadataNode) descriptors.item(0);
                width = Integer.parseInt(descriptor.getAttribute("logicalScreenWidth"));
                height = Integer.parseInt(descriptor.getAttribute("logicalScreenHeight"));
            }
        }

        if (width <= 0) width = reader.getWidth(0);
        if (height <= 0) height = reader.getHeight(0);
        return new int[] { width, height };
    }

    private static FrameMeta readGifFrameMeta(ImageReader reader, int frameIndex)
        throws IOException {
        IIOMetadataNode root = (IIOMetadataNode) reader
            .getImageMetadata(frameIndex)
            .getAsTree("javax_imageio_gif_image_1.0");

        int x = 0;
        int y = 0;
        int delay = 10;
        String disposal = "none";

        NodeList gceNodes = root.getElementsByTagName("GraphicControlExtension");
        if (gceNodes.getLength() > 0) {
            IIOMetadataNode gce = (IIOMetadataNode) gceNodes.item(0);
            delay = parseInt(gce.getAttribute("delayTime"), delay);
            disposal = gce.getAttribute("disposalMethod");
        }

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if ("ImageDescriptor".equals(node.getNodeName())) {
                NamedNodeMap map = node.getAttributes();
                x = parseInt(map.getNamedItem("imageLeftPosition").getNodeValue(), 0);
                y = parseInt(map.getNamedItem("imageTopPosition").getNodeValue(), 0);
                break;
            }
        }

        return new FrameMeta(x, y, delay, disposal);
    }

    private static int delayCentisecondsToTicks(int delayCentiseconds) {
        if (delayCentiseconds <= 0) return DEFAULT_FRAME_TICKS;
        return Math.max(1, Math.round(delayCentiseconds / 5.0F));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static BufferedImage ensureARGB(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        return copyImage(src);
    }

    private static BufferedImage copyImage(BufferedImage src) {
        BufferedImage out = new BufferedImage(
            src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    static class CapeImage {
        final BufferedImage image;
        final int frameCount;
        final int sourceWidth;
        final int sourceHeight;
        final int sourceFrameHeight;
        final boolean changed;

        CapeImage(BufferedImage image,
                  int frameCount,
                  int sourceWidth,
                  int sourceHeight,
                  int sourceFrameHeight,
                  boolean changed) {
            this.image = image;
            this.frameCount = frameCount;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.sourceFrameHeight = sourceFrameHeight;
            this.changed = changed;
        }
    }

    private static class LoadedCape {
        final BufferedImage image;
        final int[] frameTicks;
        final int frameCount;
        final int sourceWidth;
        final int sourceHeight;
        final int sourceFrameHeight;
        final boolean normalizedChanged;
        final String kind;

        LoadedCape(BufferedImage image,
                   int[] frameTicks,
                   int frameCount,
                   int sourceWidth,
                   int sourceHeight,
                   int sourceFrameHeight,
                   boolean normalizedChanged,
                   String kind) {
            this.image = image;
            this.frameTicks = frameTicks;
            this.frameCount = frameCount;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.sourceFrameHeight = sourceFrameHeight;
            this.normalizedChanged = normalizedChanged;
            this.kind = kind;
        }
    }

    private static class FrameMeta {
        final int x;
        final int y;
        final int delayCentiseconds;
        final String disposalMethod;

        FrameMeta(int x, int y, int delayCentiseconds, String disposalMethod) {
            this.x = x;
            this.y = y;
            this.delayCentiseconds = delayCentiseconds;
            this.disposalMethod = disposalMethod;
        }
    }
}

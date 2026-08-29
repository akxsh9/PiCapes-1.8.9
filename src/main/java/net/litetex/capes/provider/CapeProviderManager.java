package net.litetex.capes.provider;

import net.litetex.capes.CapeProviderX;
import net.litetex.capes.config.CapeConfig;
import net.litetex.capes.provider.providers.*;
import net.litetex.capes.texture.CapeTextureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CapeProviderManager {

    private static final ResourceLocation NO_CAPE =
        new ResourceLocation("capeproviderx", "no_cape_sentinel");

    private static CapeProviderManager INSTANCE;

    private final List<ICapeProvider> providers = new ArrayList<>();
    private final Map<String, ResourceLocation> textureCache  = new ConcurrentHashMap<>();
    private final Set<String> pendingKeys =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final File configDir;

    private volatile boolean onlyOwnCape = false;
    private volatile boolean animatedTexturesEnabled = true;

    private CapeProviderManager(File configDir, CapeConfig config) {
        this.configDir = configDir;
        this.onlyOwnCape = config.isOnlyOwnCape();
        this.animatedTexturesEnabled = config.isAnimatedTextures();
        initProviders(config);
    }

    public static void init(File configDir, CapeConfig config) {
        INSTANCE = new CapeProviderManager(configDir, config);
    }

    public static @Nullable CapeProviderManager getInstance() {
        return INSTANCE;
    }

    private void initProviders(CapeConfig config) {
    Map<String, ICapeProvider> builtIn = new LinkedHashMap<String, ICapeProvider>();
    builtIn.put("local",          new LocalFileProvider(configDir));
    builtIn.put("picapes",        new PiCapesProvider());
    builtIn.put("optifine",       new OptifineProvider());
    builtIn.put("minecraftcapes", new MinecraftCapesProvider());
    builtIn.put("skinmc",         new SkinMCProvider());
    builtIn.put("cosmetica",      new CosmeticaProvider());

    Set<String> configIds = new LinkedHashSet<String>();
    for (CapeConfig.ProviderEntry entry : config.getProviders()) {
        configIds.add(entry.getId());
        if (!entry.isEnabled()) continue;
        if ("local".equals(entry.getId()) && !config.isLocalProviders()) continue;

        ICapeProvider p = builtIn.get(entry.getId());
        if (p != null) providers.add(p);
    }

    for (Map.Entry<String, ICapeProvider> e : builtIn.entrySet()) {
        if (!configIds.contains(e.getKey())) {
            if ("local".equals(e.getKey()) && !config.isLocalProviders()) continue;
            providers.add(e.getValue());
        }
    }

    if (config.isExternalProviders()) {
        for (CapeConfig.RemoteCustomProviderEntry r : config.getRemoteCustomProviders()) {
            providers.add(new RemoteCustomProvider(r.getId(), r.getName(), r.getUriTemplate()));
        }
    }

    if (config.isModProviders()) {
        ServiceLoader<ICapeProvider> sl = ServiceLoader.load(ICapeProvider.class);
        for (ICapeProvider sp : sl) {
            providers.add(sp);
            CapeProviderX.LOGGER.info("[CapeProviderX] Loaded programmatic provider: {}", sp.getId());
        }
    }

    CapeProviderX.LOGGER.info("[CapeProviderX] Active provider order:");
    for (ICapeProvider p : providers) {
        CapeProviderX.LOGGER.info("  [{}] {}", p.getId(), p.getName());
    }
}

    public int getProviderCount() {
        return providers.size();
    }

    @Nullable
    public ResourceLocation getCapeTexture(AbstractClientPlayer player) {
    	if (!shouldHandlePlayer(player)) return null;
        String key = playerKey(player);
        ResourceLocation loc = textureCache.get(key);
        return loc == NO_CAPE ? null : loc;
    }

    public void requestCape(final AbstractClientPlayer player) {
    	if (!shouldHandlePlayer(player)) return;
        final String key = playerKey(player);
        if (textureCache.containsKey(key) || pendingKeys.contains(key)) return;
        pendingKeys.add(key);

        final com.mojang.authlib.GameProfile profile = player.getGameProfile();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ProviderResult result = findUrl(profile);
                    if (result != null) {
                        CapeTextureLoader.loadTexture(
                            key,
                            result.url,
                            result.userAgent,
                            animatedTexturesEnabled,
                            new CapeTextureLoader.TextureCallback() {
                                @Override
                                public void onLoaded(ResourceLocation location) {
                                    textureCache.put(key, location);
                                    pendingKeys.remove(key);
                                }
                            }
                        );
                    } else {
                        textureCache.put(key, NO_CAPE);
                        pendingKeys.remove(key);
                    }
                } catch (Exception e) {
                    CapeProviderX.LOGGER.warn(
                        "[CapeProviderX] Failed loading cape for {}: {}",
                        key, e.getMessage());
                    pendingKeys.remove(key);
                }
            }
        }, "CapeProviderX-" + key.substring(0, Math.min(8, key.length())));
        t.setDaemon(true);
        t.start();
    }

    private static String playerKey(AbstractClientPlayer player) {
        UUID uuid = player.getGameProfile().getId();
        if (uuid != null) return uuid.toString();
        return "name:" + player.getGameProfile().getName();
    }

    public void clearCache() {
        textureCache.clear();
        pendingKeys.clear();
        CapeProviderX.LOGGER.info("[CapeProviderX] Cape cache cleared.");
    }

    public void refreshPlayer(AbstractClientPlayer player) {
        String key = playerKey(player);
        textureCache.remove(key);
        pendingKeys.remove(key);
        CapeProviderX.LOGGER.info("[CapeProviderX] Cape refresh queued for {}", key);
    }
        
    private static class ProviderResult {
        final String url;
        final String userAgent;

        ProviderResult(String url, String userAgent) {
            this.url       = url;
            this.userAgent = userAgent;
        }
    }
    
    private boolean shouldHandlePlayer(AbstractClientPlayer player) {
        if (!onlyOwnCape) return true;
        return Minecraft.getMinecraft().thePlayer == player;
    }

    private ProviderResult findUrl(com.mojang.authlib.GameProfile profile) {
        for (ICapeProvider p : providers) {
            try {
                String url = p.getCapeUrl(profile);
                if (url != null && !url.isEmpty()) {
                    CapeProviderX.LOGGER.debug(
                        "[CapeProviderX] {} -> {} from {}",
                        profile.getName(), url, p.getId());
                    return new ProviderResult(url, p.getUserAgent());
                }
            } catch (Exception e) {
                CapeProviderX.LOGGER.debug(
                    "[CapeProviderX] Provider {} threw for {}: {}",
                    p.getId(), profile.getName(), e.getMessage());
            }
        }
        return null;
    }
}

package net.litetex.capes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.litetex.capes.CapeProviderX;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CapeConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private List<ProviderEntry>             providers;
    private List<RemoteCustomProviderEntry> remoteCustomProviders;
    
    private boolean onlyOwnCape        = false;
    private boolean animatedTextures   = true;
    private boolean externalProviders  = true;
    private boolean modProviders       = true;
    private boolean localProviders     = true;

    public CapeConfig() {
        providers = new ArrayList<>(Arrays.asList(
            new ProviderEntry("local",          true),
            new ProviderEntry("picapes",        true),
            new ProviderEntry("optifine",       true),
            new ProviderEntry("minecraftcapes", true),
            new ProviderEntry("skinmc",         true),
            new ProviderEntry("cosmetica",      true)
        ));
        remoteCustomProviders = new ArrayList<>();
    }

    public List<ProviderEntry>             getProviders()             { return providers; }
    public List<RemoteCustomProviderEntry> getRemoteCustomProviders() { return remoteCustomProviders; }

    public boolean isOnlyOwnCape()       { return onlyOwnCape; }
    public boolean isAnimatedTextures()  { return animatedTextures; }
    public boolean isExternalProviders() { return externalProviders; }
    public boolean isModProviders()      { return modProviders; }
    public boolean isLocalProviders()    { return localProviders; }

    public void setOnlyOwnCape(boolean v)       { this.onlyOwnCape = v; }
    public void setAnimatedTextures(boolean v)  { this.animatedTextures = v; }
    public void setExternalProviders(boolean v) { this.externalProviders = v; }
    public void setModProviders(boolean v)      { this.modProviders = v; }
    public void setLocalProviders(boolean v)    { this.localProviders = v; }

    public static CapeConfig load(File configDir) {
        File file = new File(configDir, "config.json");
        if (!file.exists()) {
            CapeConfig defaults = new CapeConfig();
            defaults.save(configDir);
            return defaults;
        }
        try (Reader r = new FileReader(file)) {
            CapeConfig cfg = GSON.fromJson(r, CapeConfig.class);
            if (cfg == null)                       cfg = new CapeConfig();
            if (cfg.providers == null)             cfg.providers = new ArrayList<>();
            if (cfg.remoteCustomProviders == null) cfg.remoteCustomProviders = new ArrayList<>();
            return cfg;
        } catch (Exception e) {
            CapeProviderX.LOGGER.error("[CapeProviderX] Could not read config: {}", e.getMessage());
            return new CapeConfig();
        }
    }

    public void save(File configDir) {
        File file = new File(configDir, "config.json");
        try (Writer w = new FileWriter(file)) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            CapeProviderX.LOGGER.error("[CapeProviderX] Could not write config: {}", e.getMessage());
        }
    }

    public static class ProviderEntry {
        private String  id;
        private boolean enabled;

        public ProviderEntry(String id, boolean enabled) {
            this.id      = id;
            this.enabled = enabled;
        }

        public String  getId()                   { return id; }
        public boolean isEnabled()               { return enabled; }
        public void    setEnabled(boolean value) { this.enabled = value; }
    }

    public static class RemoteCustomProviderEntry {
        private String id;
        private String name;
        private String uriTemplate;

        public String getId()          { return id; }
        public String getName()        { return name != null ? name : id; }
        public String getUriTemplate() { return uriTemplate; }
    }
}
package net.litetex.capes.provider.providers;

import com.mojang.authlib.GameProfile;
import net.litetex.capes.provider.ICapeProvider;

import javax.annotation.Nullable;

public class SkinMCProvider implements ICapeProvider {
    @Override public String getId()   { return "skinmc"; }
    @Override public String getName() { return "SkinMC"; }

    @Override
    @Nullable
    public String getCapeUrl(GameProfile profile) {
        if (profile.getId() == null) return null;
        return "https://skinmc.net/api/v1/skinmcCape/" + profile.getId().toString();
    }
    
    @Override
    public String getUserAgent() {
        return "CapeProviderX/1.0 Forge/1.8.9";
    }
}

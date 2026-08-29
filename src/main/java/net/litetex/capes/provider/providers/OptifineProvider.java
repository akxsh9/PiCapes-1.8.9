package net.litetex.capes.provider.providers;

import com.mojang.authlib.GameProfile;
import net.litetex.capes.provider.ICapeProvider;

import javax.annotation.Nullable;

public class OptifineProvider implements ICapeProvider {
    @Override public String getId()   { return "optifine"; }
    @Override public String getName() { return "Optifine"; }

    @Override
    @Nullable
    public String getCapeUrl(GameProfile profile) {
        return "http://s.optifine.net/capes/" + profile.getName() + ".png";
    }
    
    @Override
    public String getUserAgent() {
        return "CapeProviderX/1.0 Forge/1.8.9";
    }
}
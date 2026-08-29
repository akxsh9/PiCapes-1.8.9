package net.litetex.capes.provider.providers;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.GameProfile;
import net.litetex.capes.provider.ICapeProvider;

import javax.annotation.Nullable;

public class MinecraftCapesProvider implements ICapeProvider {

    private static final Gson GSON = new Gson();

    @Override public String getId()   { return "minecraftcapes"; }
    @Override public String getName() { return "MinecraftCapes"; }

    @Override
    @Nullable
    public String getCapeUrl(GameProfile profile) {
        if (profile.getId() == null) return null;

        try {
            String uuid = profile.getId().toString().replace("-", "");
            String json = ProviderHttp.getText("https://api.minecraftcapes.net/profile/" + uuid);
            if (json == null) return null;

            ResponseData response = GSON.fromJson(json, ResponseData.class);
            if (response == null) return null;

            if (response.animatedCapeUrl != null && !response.animatedCapeUrl.isEmpty()) {
                return response.animatedCapeUrl;
            }
            return response.capeUrl;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String getUserAgent() {
        return "CapeProviderX/1.0 Forge/1.8.9";
    }

    private static class ResponseData {
        @SerializedName("cape_url")
        String capeUrl;

        @SerializedName("animated_cape_url")
        String animatedCapeUrl;
    }
}

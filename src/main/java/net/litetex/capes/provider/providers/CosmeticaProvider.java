package net.litetex.capes.provider.providers;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import net.litetex.capes.provider.ICapeProvider;

import javax.annotation.Nullable;

public class CosmeticaProvider implements ICapeProvider {

    private static final Gson GSON = new Gson();
    private static final String BASE64_PREFIX = "data:image/png;base64,";

    @Override public String getId()   { return "cosmetica"; }
    @Override public String getName() { return "Cosmetica"; }

    @Override
    @Nullable
    public String getCapeUrl(GameProfile profile) {
        if (profile.getId() == null) return null;

        try {
            String json = ProviderHttp.getText(
                "https://api.cosmetica.cc/v2/get/info?uuid=" + profile.getId().toString()
                    + "&nothirdparty&excludemodels");
            if (json == null) return null;

            ResponseData response = GSON.fromJson(json, ResponseData.class);
            if (response == null || response.cape == null || response.cape.image == null) return null;
            if (!response.cape.image.startsWith(BASE64_PREFIX)) return null;

            return response.cape.image;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String getUserAgent() {
        return "CapeProviderX/1.0 Forge/1.8.9";
    }

    private static class ResponseData {
        CapeData cape;
    }

    private static class CapeData {
        String image;
    }
}

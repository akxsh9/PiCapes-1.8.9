package net.litetex.capes.provider.providers;

import com.mojang.authlib.GameProfile;
import net.litetex.capes.provider.ICapeProvider;

import javax.annotation.Nullable;

public class RemoteCustomProvider implements ICapeProvider {
    private final String id;
    private final String name;
    private final String uriTemplate;

    public RemoteCustomProvider(String id, String name, String uriTemplate) {
        this.id          = id;
        this.name        = name;
        this.uriTemplate = uriTemplate;
    }

    @Override public String getId()   { return id; }
    @Override public String getName() { return name != null ? name : id; }

    @Override
    @Nullable
    public String getCapeUrl(GameProfile profile) {
        if (uriTemplate == null || uriTemplate.isEmpty()) return null;
        String uuid       = profile.getId() != null ? profile.getId().toString() : "";
        String uuidNoDash = uuid.replace("-", "");
        return uriTemplate
            .replace("$uuid",       uuid)
            .replace("§uuid",       uuid)   // escaped variant from original mod
            .replace("$id",         uuid)
            .replace("§id",         uuid)
            .replace("$idNoHyphen", uuidNoDash)
            .replace("§idNoHyphen", uuidNoDash)
            .replace("$name",       profile.getName())
            .replace("§name",       profile.getName());
    }
    
    @Override
    public String getUserAgent() {
        return "CapeProviderX/1.0 Forge/1.8.9";
    }
}
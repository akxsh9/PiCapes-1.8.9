package net.litetex.capes.provider;

import com.mojang.authlib.GameProfile;

public interface ICapeProvider {

    String getId();
    String getName();
    String getCapeUrl(GameProfile profile);
    String getUserAgent();
}
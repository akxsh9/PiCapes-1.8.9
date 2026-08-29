package net.litetex.capes.provider.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.litetex.capes.CapeProviderX;
import net.litetex.capes.provider.ICapeProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PiCapesProvider implements ICapeProvider {

    private static final String PROFILE_API  = "https://capeserver.picapes.syanic.org/profile/";
    private static final String USER_AGENT   = "picapes-legacy-mod/1.0";
    private static final int    TIMEOUT_MS   = 5000;

    @Override public String getId()   { return "picapes"; }
    @Override public String getName() { return "PiCapes"; }

    @Override
    public String getCapeUrl(GameProfile profile) {
        String name = profile.getName();
        if (name == null || name.isEmpty()) return null;

        try {
            URL url = new URL(PROFILE_API + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            int code = conn.getResponseCode();
            if (code == 404) {
                return null;
            }
            if (code != 200) {
                CapeProviderX.LOGGER.info(
                    "[CapeProviderX] PiCapes profile API returned {} for {}", name, code);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();

            return parseCapeUrl(sb.toString(), name);

        } catch (Exception e) {
            CapeProviderX.LOGGER.info(
                "[CapeProviderX] PiCapes profile fetch failed for {}: {}", name, e.getMessage());
            return null;
        }
    }

    private String parseCapeUrl(String json, String playerName) {
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();

            boolean animated = root.has("animatedCape")
                && !root.get("animatedCape").isJsonNull()
                && root.get("animatedCape").getAsBoolean();

            if (animated) {
                if (root.has("textureURL") && !root.get("textureURL").isJsonNull()) {
                    String url = root.get("textureURL").getAsString();
                    if (!url.isEmpty()) {
                        CapeProviderX.LOGGER.info(
                            "[CapeProviderX] PiCapes: animated cape for {} -> {}", playerName, url);
                        return url;
                    }
                }
            }

            if (root.has("staticURL") && !root.get("staticURL").isJsonNull()) {
                String url = root.get("staticURL").getAsString();
                if (!url.isEmpty()) {
                    CapeProviderX.LOGGER.info(
                        "[CapeProviderX] PiCapes: static cape for {} -> {}", playerName, url);
                    return url;
                }
            }

            if (root.has("textureURL") && !root.get("textureURL").isJsonNull()) {
                String url = root.get("textureURL").getAsString();
                if (!url.isEmpty()) return url;
            }

            CapeProviderX.LOGGER.info(
                "[CapeProviderX] PiCapes: no usable URL found for {}. JSON: {}", playerName, json);
            return null;

        } catch (Exception e) {
            CapeProviderX.LOGGER.warn(
                "[CapeProviderX] PiCapes JSON parse failed for {}: {}", playerName, e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getUserAgent() {
        return "picapes-legacy-mod/1.0";
    }
}
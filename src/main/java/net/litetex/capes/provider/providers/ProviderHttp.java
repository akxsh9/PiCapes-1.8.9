package net.litetex.capes.provider.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ProviderHttp {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS    = 10_000;

    private ProviderHttp() {}

    static String getText(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "CapeProviderX/1.0 Forge/1.8.9");
        conn.setInstanceFollowRedirects(true);

        try {
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) return null;

            try (InputStream is = conn.getInputStream()) {
                return readFully(is);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static String readFully(InputStream is) throws IOException {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(is, StandardCharsets.UTF_8))) {
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                out.append(buffer, 0, read);
            }
        }
        return out.toString();
    }
}

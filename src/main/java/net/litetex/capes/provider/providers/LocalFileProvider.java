package net.litetex.capes.provider.providers;

import com.mojang.authlib.GameProfile;
import net.litetex.capes.CapeProviderX;
import net.litetex.capes.provider.ICapeProvider;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

public class LocalFileProvider implements ICapeProvider {

    private final File configDir;

    private final List<SimpleCustomEntry> subProviders = new ArrayList<>();

    public LocalFileProvider(File configDir) {
        this.configDir = configDir;
        loadSubProviders();
    }

    @Override public String getId()   { return "local"; }
    @Override public String getName() { return "Local File"; }

    @Override
    @Nullable
    public String getCapeUrl(GameProfile profile) {
        //cape.png (applies to ALL players unless owners.txt exists)
        File rootCape = new File(configDir, "cape.png");
        if (rootCape.exists()) {
            File ownersFile = new File(configDir, "owners.txt");
            if (!ownersFile.exists() || isOwner(ownersFile, profile)) {
                return rootCape.toURI().toString();
            }
        }

        for (SimpleCustomEntry entry : subProviders) {
            if (entry.capeFile.exists()) {
                if (entry.owners == null || entry.owners.isEmpty() || matchesOwner(entry.owners, profile)) {
                    return entry.capeFile.toURI().toString();
                }
            }
        }

        return null;
    }


    private void loadSubProviders() {
        File simpleCustomDir = new File(configDir, "simple-custom");
        if (!simpleCustomDir.exists() || !simpleCustomDir.isDirectory()) return;

        File[] dirs = simpleCustomDir.listFiles(File::isDirectory);
        if (dirs == null) return;

        for (File dir : dirs) {
            File capeFile = new File(dir, "cape.png");
            if (!capeFile.exists()) continue;

            String displayName = dir.getName();
            File nameFile = new File(dir, "name.txt");
            if (nameFile.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(nameFile))) {
                    String line = r.readLine();
                    if (line != null && !line.trim().isEmpty()) displayName = line.trim();
                } catch (IOException e) {
                    CapeProviderX.LOGGER.warn("[CapeProviderX] Could not read name.txt in {}", dir.getName());
                }
            }

            Set<String> owners = null;
            File ownersFile = new File(dir, "owners.txt");
            if (ownersFile.exists()) {
                owners = readOwners(ownersFile);
            }

            subProviders.add(new SimpleCustomEntry(displayName, capeFile, owners));
            CapeProviderX.LOGGER.info("[CapeProviderX] Loaded simple-custom provider: {}", displayName);
        }
    }

    private boolean isOwner(File ownersFile, GameProfile profile) {
        Set<String> owners = readOwners(ownersFile);
        return matchesOwner(owners, profile);
    }

    private boolean matchesOwner(Set<String> owners, GameProfile profile) {
        if (owners == null || owners.isEmpty()) return true;
        String uuidStr = profile.getId() != null ? profile.getId().toString() : null;
        String name    = profile.getName() != null ? profile.getName().toLowerCase() : null;
        for (String entry : owners) {
            if (uuidStr != null && uuidStr.equalsIgnoreCase(entry)) return true;
            if (name    != null && name.equalsIgnoreCase(entry))    return true;
        }
        return false;
    }

    private static Set<String> readOwners(File file) {
        Set<String> result = new HashSet<>();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    result.add(line.toLowerCase());
                }
            }
        } catch (IOException e) {
            CapeProviderX.LOGGER.warn("[CapeProviderX] Could not read owners.txt: {}", e.getMessage());
        }
        return result;
    }
    
    @Override
    public String getUserAgent() {
        return "CapeProviderX/1.0 Forge/1.8.9";
    }

    private static class SimpleCustomEntry {
        final String      name;
        final File        capeFile;
        final Set<String> owners;

        SimpleCustomEntry(String name, File capeFile, Set<String> owners) {
            this.name     = name;
            this.capeFile = capeFile;
            this.owners   = owners;
        }
    }
}
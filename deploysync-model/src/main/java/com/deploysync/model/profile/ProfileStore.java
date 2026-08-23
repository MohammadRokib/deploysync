package com.deploysync.model.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class ProfileStore {
    private static final int FILE_VERSION = 1;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path storeFile;

    ProfileStore(Path storeFile) {
        this.storeFile = storeFile;
    }

    public ProfileStore() {
        this(resolveDefaultStoreFile());
    }

    public static Path resolveDefaultStoreFile() {
        String appDir = System.getProperty("deploysync.appdir");
        Path base = (appDir != null && !appDir.isBlank())
                ? Path.of(appDir)
                : Path.of(System.getProperty("user.dir"));
        return base.resolve("profiles.json");
    }

    public synchronized List<EnvironmentProfile> list() {
        return new ArrayList<>(readAll().values());
    }

    public synchronized Optional<EnvironmentProfile> find(String name) {
        return Optional.ofNullable(readAll().get(name));
    }

    public synchronized void save(EnvironmentProfile profile) {
        Map<String, EnvironmentProfile> profiles = readAll();
        profiles.put(profile.name(), profile);
        writeAll(profiles.values());
    }

    public synchronized void delete(String name) {
        Map<String, EnvironmentProfile> profiles = readAll();
        profiles.remove(name);
        writeAll(profiles.values());
    }

    public synchronized String suggestDuplicateName(String baseName) {
        Set<String> existing = readAll().keySet();
        String candidate = baseName + " (Copy)";
        int suffix = 2;
        while(existing.contains(candidate)) {
            candidate = baseName + " (Copy " + suffix + ")";
            suffix++;
        }
        return candidate;
    }

    private Map<String, EnvironmentProfile> readAll() {
        if (!Files.isRegularFile(storeFile)) {
            return new LinkedHashMap<>();
        }

        try {
            ProfilesFile file = objectMapper.readValue(storeFile.toFile(), ProfilesFile.class);
            Map<String, EnvironmentProfile> byName = new LinkedHashMap<>();

            for (EnvironmentProfile profile : file.profiles()) {
                byName.put(profile.name(), profile);
            }
            return byName;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read profiles file: " + storeFile, e);
        }
    }

    private void writeAll(Collection<EnvironmentProfile> profiles) {
        try {
            Files.createDirectories(storeFile.getParent());
            Path tempFile = storeFile.resolveSibling(storeFile.getFileName() + ".tmp");
            objectMapper.writeValue(tempFile.toFile(), new ProfilesFile(FILE_VERSION, new ArrayList<>(profiles)));
            Files.move(tempFile, storeFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write profiles file: " + storeFile, e);
        }
    }

    private record ProfilesFile(int version, List<EnvironmentProfile> profiles) {}
}

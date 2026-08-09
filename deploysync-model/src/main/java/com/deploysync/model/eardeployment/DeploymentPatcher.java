package com.deploysync.model.eardeployment;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.function.BiConsumer;

@Service
public class DeploymentPatcher {
    public DeploymentResult patch(Path masterEarFile, Path filesFolder, List<ManifestEntry> entries,
                                  BiConsumer<Integer, Integer> onProgress) {
        Path workingCopy = masterEarFile.resolveSibling(masterEarFile.getFileName() + ".working");

        try {
            Files.copy(masterEarFile, workingCopy, StandardCopyOption.REPLACE_EXISTING);

            int completed = 0;
            for (ManifestEntry entry : entries) {
                byte[] sourceBytes = Files.readAllBytes(filesFolder.resolve(entry.source()));
                List<String> segments = NestedArchivePathResolver.resolveEntrySegments(entry.target(), entry.source());

                NestedArchivePatcher.writeEntry(workingCopy, segments, sourceBytes);
                byte[] writtenBytes = NestedArchivePatcher.readEntry(workingCopy, segments);

                if (!sha256(sourceBytes).equals(sha256(writtenBytes))) {
                    Files.deleteIfExists(workingCopy);
                    return new DeploymentResult(false,
                            "Hash mismatch after patching '" + entry.target() + "' - deployment aborted, live file untouched.");
                }

                completed++;
                onProgress.accept(completed, entries.size());
            }

            Path backupDir = masterEarFile.getParent().resolve(
                    "backup-" +
                            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
            );

            Files.createDirectories(backupDir);
            Path backupFile = backupDir.resolve(masterEarFile.getFileName());
            Files.copy(masterEarFile, backupFile);

            Files.move(workingCopy, masterEarFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            return new DeploymentResult(true,
                    entries.size() + " file(s) patched and verified. Backup: " + backupFile);
        } catch (IOException e) {
            deleteQuietly(workingCopy);
            return new DeploymentResult(false,
                                       "Patch failed: " + e.getMessage() + " - live file untouched.");
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

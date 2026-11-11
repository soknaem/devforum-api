package com.setec.forum.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Default StorageService implementation: saves files under `storage.base-path` on local disk,
 * organized as {subFolder}/{yyyy}/{MM}/{uuid}.{ext}, and serves them back via the static resource
 * handler registered in WebMvcStaticConfig at `storage.base-url` + "/{storageKey}".
 */
@Service
@RequiredArgsConstructor
public class LocalDiskStorageService implements StorageService {

    private final StorageProperties properties;

    @Override
    public StoredFile store(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = extractExtension(originalFileName);
        validateExtension(extension);
        validateSize(file.getSize());

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storageKey = subFolder + "/" + datePath + "/" + UUID.randomUUID() + "." + extension;

        Path targetPath = Path.of(properties.getBasePath(), storageKey);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store uploaded file", ex);
        }

        String url = properties.getBaseUrl() + "/" + storageKey;
        return new StoredFile(storageKey, url, originalFileName, file.getContentType(), file.getSize());
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(Path.of(properties.getBasePath(), storageKey));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to delete stored file: " + storageKey, ex);
        }
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new IllegalArgumentException("Uploaded file must have an extension");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateExtension(String extension) {
        List<String> allowed = properties.getAllowedExtensions();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(extension)) {
            throw new IllegalArgumentException("File type ." + extension + " is not allowed. Allowed: " + allowed);
        }
    }

    private void validateSize(long sizeBytes) {
        long maxBytes = properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (sizeBytes > maxBytes) {
            throw new IllegalArgumentException("File exceeds the maximum allowed size of "
                    + properties.getMaxFileSizeMb() + "MB");
        }
    }
}

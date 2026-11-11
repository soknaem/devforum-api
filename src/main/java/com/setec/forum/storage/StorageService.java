package com.setec.forum.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Persists the given file and returns where it landed. Implementations decide the physical
     * location (local disk today; swap for an S3Client-backed implementation later without
     * touching any controller or service that calls this interface).
     *
     * @param file      the uploaded multipart file
     * @param subFolder logical grouping, e.g. "resources", "avatars", "covers"
     */
    StoredFile store(MultipartFile file, String subFolder);

    void delete(String storageKey);
}

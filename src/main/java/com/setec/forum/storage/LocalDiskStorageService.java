package com.setec.forum.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Local disk storage with bounded resizing and recompression for raster photo uploads. */
@Service
@RequiredArgsConstructor
public class LocalDiskStorageService implements StorageService {

    private static final int MAX_IMAGE_EDGE = 1920;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final float JPEG_QUALITY = 0.82f;

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

        byte[] content = readAndOptimizeImage(file, extension);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storageKey = subFolder + "/" + datePath + "/" + UUID.randomUUID() + "." + extension;
        Path targetPath = Path.of(properties.getBasePath(), storageKey);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store uploaded file", ex);
        }

        String url = properties.getBaseUrl() + "/" + storageKey;
        return new StoredFile(storageKey, url, originalFileName, safeContentType(extension, file.getContentType()), content.length);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(Path.of(properties.getBasePath(), storageKey));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to delete stored file: " + storageKey, ex);
        }
    }

    private byte[] readAndOptimizeImage(MultipartFile file, String extension) {
        if (!List.of("jpg", "jpeg", "png").contains(extension)) {
            try {
                return file.getBytes();
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to read uploaded file", ex);
            }
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            if (input == null) return file.getBytes();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return file.getBytes();

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions are too large to process");
                }
                int sample = Math.max(1, (int) Math.ceil((double) Math.max(width, height) / MAX_IMAGE_EDGE));
                var param = reader.getDefaultReadParam();
                param.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage decoded = reader.read(0, param);
                if (decoded == null) return file.getBytes();

                double scale = Math.min(1d, (double) MAX_IMAGE_EDGE / Math.max(decoded.getWidth(), decoded.getHeight()));
                int outWidth = Math.max(1, (int) Math.round(decoded.getWidth() * scale));
                int outHeight = Math.max(1, (int) Math.round(decoded.getHeight() * scale));
                boolean jpeg = extension.equals("jpg") || extension.equals("jpeg");
                BufferedImage optimized = resize(decoded, outWidth, outHeight, jpeg);
                return encode(optimized, jpeg ? "jpeg" : "png", jpeg);
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not decode uploaded image", ex);
        }
    }

    private BufferedImage resize(BufferedImage source, int width, int height, boolean jpeg) {
        int type = jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage output = new BufferedImage(width, height, type);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (jpeg) {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, width, height);
        }
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return output;
    }

    private byte[] encode(BufferedImage image, String format, boolean jpeg) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) throw new IOException("No image encoder available for " + format);
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (jpeg && params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(JPEG_QUALITY);
                if (params.canWriteProgressive()) params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            }
            writer.write(null, new IIOImage(image, null, null), params);
            output.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private String safeContentType(String extension, String provided) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> provided;
        };
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

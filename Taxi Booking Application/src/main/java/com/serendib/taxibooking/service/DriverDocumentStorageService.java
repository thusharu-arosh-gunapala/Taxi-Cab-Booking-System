package com.serendib.taxibooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
public class DriverDocumentStorageService {

    private static final Logger logger = LoggerFactory.getLogger(DriverDocumentStorageService.class);
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private final Path storageDirectory;

    public DriverDocumentStorageService(
            @Value("${app.driver.upload-directory:./var/driver-uploads}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String storeRequiredImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload all required profile, licence, and vehicle images.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Each driver image must be 5 MB or smaller.");
        }
        String extension = extensionFor(file.getContentType());
        String key = UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            byte[] content = file.getBytes();
            if (!matchesImageSignature(content, extension)) {
                throw new IllegalArgumentException("The uploaded file does not match its image format.");
            }
            Files.createDirectories(storageDirectory);
            Path target = resolve(key);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
            return key;
        } catch (IOException exception) {
            throw new IllegalStateException("Driver image could not be stored securely.", exception);
        }
    }

    public Resource load(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("The requested driver image is unavailable.");
        }
        Path path = resolve(key);
        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("The requested driver image is unavailable.");
        }
        return resource;
    }

    public MediaType mediaType(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            logger.warn("Could not remove private driver file {}", key, exception);
        }
    }

    private String extensionFor(String contentType) {
        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return ".jpg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        throw new IllegalArgumentException("Upload images in JPG, PNG, or WebP format only.");
    }

    private boolean matchesImageSignature(byte[] content, String extension) {
        return switch (extension) {
            case ".jpg" -> content.length >= 3
                    && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
                    && (content[2] & 0xFF) == 0xFF;
            case ".png" -> content.length >= 8
                    && (content[0] & 0xFF) == 0x89 && content[1] == 0x50
                    && content[2] == 0x4E && content[3] == 0x47
                    && content[4] == 0x0D && content[5] == 0x0A
                    && content[6] == 0x1A && content[7] == 0x0A;
            case ".webp" -> content.length >= 12
                    && content[0] == 0x52 && content[1] == 0x49
                    && content[2] == 0x46 && content[3] == 0x46
                    && content[8] == 0x57 && content[9] == 0x45
                    && content[10] == 0x42 && content[11] == 0x50;
            default -> false;
        };
    }

    private Path resolve(String key) {
        if (!key.matches("^[a-fA-F0-9]{32}\\.(jpg|png|webp)$")) {
            throw new IllegalArgumentException("The requested driver image is unavailable.");
        }
        Path path = storageDirectory.resolve(key).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("The requested driver image is unavailable.");
        }
        return path;
    }
}

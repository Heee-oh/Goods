package com.goods.market.listing.application;

import com.goods.market.listing.exception.ListingBadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ListingImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp"
    );

    private final Path baseDir;

    public ListingImageStorageService(@Value("${app.storage.base-dir:uploads}") String baseDir) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        return store(file, "listing-images");
    }

    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new ListingBadRequestException("Invalid image file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ListingBadRequestException("Only image files are allowed");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        String folder = subDirectory == null || subDirectory.isBlank() ? "listing-images" : subDirectory;
        Path targetDir = baseDir.resolve(folder);
        Path targetFile = targetDir.resolve(filename);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException e) {
            log.error("Failed to store image in {}", folder, e);
            throw new IllegalStateException("Failed to store image", e);
        }

        return "/uploads/" + folder + "/" + filename;
    }

    // 확장자 추출
    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (extension.length() <= 10) {
                    return extension;
                }
            }
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            default -> "";
        };
    }
}

package com.goods.market.listing.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ListingImageStorageServiceTest {

    @Test
    void storeWritesImageUnderConfiguredDirectory(@TempDir Path tempDir) {
        ListingImageStorageService service = new ListingImageStorageService(tempDir.toString());

        MultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        String imageUrl = service.store(file);

        assertThat(imageUrl).startsWith("/uploads/listing-images/");
        Path storedFile = tempDir.resolve("listing-images").resolve(imageUrl.substring(imageUrl.lastIndexOf('/') + 1));
        assertThat(Files.exists(storedFile)).isTrue();
    }
}

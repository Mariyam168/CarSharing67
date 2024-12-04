package com.example.carsharing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FileUploadService {
    @Value("${file.upload-dir}")
    private String uploadDir;
    private final ResourceLoader resourceLoader;

    public String storeFile(MultipartFile file) throws IOException {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Save the file
        File destinationFile = new File(directory, file.getOriginalFilename());
        file.transferTo(destinationFile);
        return destinationFile.getAbsolutePath();
    }
}


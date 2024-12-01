package com.example.carsharing.service;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileUploadService {
    private final Path fileStorageLocation;

    @Autowired
    public FileUploadService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Не удалось создать директорию для загрузки файлов", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            // Проверка на корректность имени файла
            if (fileName.contains("..")) {
                throw new RuntimeException("Имя файла содержит недопустимые символы: " + fileName);
            }

            // Сохраняем файл в директорию
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Возвращаем относительный путь для сохранения в базе данных
            return targetLocation.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Не удалось сохранить файл: " + fileName, ex);
        }
    }
}


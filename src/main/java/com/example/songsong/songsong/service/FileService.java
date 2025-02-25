package com.example.songsong.songsong.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    private String data = "data/";

    public String saveFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return "File is empty, cannot upload";
        }

        String fileName = file.getOriginalFilename();
        Path uploadPath = Paths.get(data);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        return "File uploaded successfully";
    }
}

package com.example.songsong.songsong.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    private String data = System.getProperty("user.dir") + "/data";

    public FileService() {
        try {
            Path uploadPath = Paths.get(data);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> listFile() {
        try {
            Path folderPath = Paths.get(data);
            if (!Files.exists(folderPath)) {
                return Collections.singletonList(Map.of("error", "Folder Data not exists"));
            }

            List<Map<String, Object>> listFiles = new ArrayList<>();
            Files.list(folderPath).forEach(file -> {
                try {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getFileName().toString());
                    fileInfo.put("size", Files.size(file));
                    listFiles.add(fileInfo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return listFiles;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.singletonList(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    public String saveFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return "File is empty, cannot upload";
        }

        String fileName = file.getOriginalFilename();
        Path uploadPath = Paths.get(data);
        Path filePath = uploadPath.resolve(fileName);

        Files.write(filePath, file.getBytes());
        return "File uploaded successfully: " + fileName;
    }
}

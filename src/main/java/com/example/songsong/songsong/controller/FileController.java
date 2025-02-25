package com.example.songsong.songsong.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.songsong.songsong.service.FileService;

@RestController
@RequestMapping("/files")
public class FileController {
    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        try {
            String fileStatus = fileService.saveFile(file);
            return ResponseEntity.ok(fileStatus);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}

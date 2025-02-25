package com.example.songsong.songsong.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.songsong.songsong.service.FileService;

@RestController
@CrossOrigin(origins = "http://127.0.0.1:5500")
@RequestMapping("/files")
public class FileController {
    @Autowired
    private FileService fileService;

    @GetMapping("/listFile")
    public ResponseEntity<?> listFile() throws Exception {
        List<Map<String, Object>> listFiles = fileService.listFile();
        return ResponseEntity.ok(listFiles);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        try {
            String fileStatus = fileService.saveFile(file);
            System.out.println("FileStatus: "+fileStatus);
            return ResponseEntity.ok(fileStatus);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}

package com.example.songsong.songsong.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class FileModel {
    @Id
    private String FileName;

    private String FileSource;

    private Double FileSize;

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public String getFileSource() {
        return FileSource;
    }

    public void setFileSource(String fileSource) {
        FileSource = fileSource;
    }

    public Double getFileSize() {
        return FileSize;
    }

    public void setFileSize(Double FileSize) {
        this.FileSize = FileSize;
    }
}

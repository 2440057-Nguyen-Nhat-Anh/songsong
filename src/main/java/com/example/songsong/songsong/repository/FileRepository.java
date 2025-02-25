package com.example.songsong.songsong.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.songsong.songsong.model.FileModel;

public interface FileRepository extends JpaRepository<FileModel, String>{
    
}

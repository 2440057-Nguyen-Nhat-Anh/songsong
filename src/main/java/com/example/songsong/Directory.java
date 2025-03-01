package com.example.songsong;

import java.util.*;

public class Directory {
    private Map<String, List<String>> fileMap = new HashMap<>();

    public synchronized void registerFile(String fileName, String client) {
        fileMap.computeIfAbsent(fileName, k -> new ArrayList<>()).add(client);
    }

    public synchronized List<String> getClientsWithFile(String fileName) {
        return fileMap.getOrDefault(fileName, Collections.emptyList());
    }

    public void displayFiles() {
        fileMap.forEach((file, clients) -> 
            System.out.println(file + " is available at: " + clients));
    }
}

package com.example.songsong;

import java.util.*;

public class Daemon {
    private String clientId;
    private Directory directory;

    public Daemon(String clientId, Directory directory) {
        this.clientId = clientId;
        this.directory = directory;
    }

    public void shareFile(String fileName) {
        directory.registerFile(fileName, clientId);
        System.out.println(clientId + " shared: " + fileName);
    }

    public void requestFile(String fileName) {
        List<String> clients = directory.getClientsWithFile(fileName);
        if (clients.isEmpty()) {
            System.out.println("File not found: " + fileName);
        } else {
            System.out.println(clientId + " is downloading " + fileName + " from " + clients);
        }
    }

    public String getClientId() {
        return clientId;
    }
}

package com.example.songsong;

import java.util.concurrent.*;

public class Download {
    public static void downloadFile(String fileName, Daemon fromDaemon) {
        System.out.println("Downloading " + fileName + " from " + fromDaemon.getClientId());
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            int part = i + 1;
            executor.submit(() -> {
                System.out.println("Downloading part " + part + " of " + fileName);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
    }
}

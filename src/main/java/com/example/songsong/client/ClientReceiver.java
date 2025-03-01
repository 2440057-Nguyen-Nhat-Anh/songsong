package com.example.songsong.client;

import java.io.*;
import java.net.Socket;

public class ClientReceiver {
    private static final int CHUNK_SIZE = 4096; // 4KB mỗi phần

    public static void downloadFile(String host, int port, String outputFilePath) {
        try (Socket socket = new Socket(host, port);
             InputStream inputStream = socket.getInputStream();
             BufferedOutputStream fileOutput = new BufferedOutputStream(new FileOutputStream(outputFilePath))) {

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutput.write(buffer, 0, bytesRead);
            }

            System.out.println("Download complete: " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

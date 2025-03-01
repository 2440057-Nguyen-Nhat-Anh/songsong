package com.example.songsong.client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientSender {
    private static final int CHUNK_SIZE = 4096; // 4KB mỗi phần

    public static void startFileServer(String filePath, int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("FileSender: Listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                try (OutputStream outputStream = clientSocket.getOutputStream();
                     BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(filePath))) {

                    byte[] buffer = new byte[CHUNK_SIZE];
                    int bytesRead;

                    // Gửi từng phần file
                    while ((bytesRead = fileInput.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    }

                    System.out.println("File sent successfully!");

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    clientSocket.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

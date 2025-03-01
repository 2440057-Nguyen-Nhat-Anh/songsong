package com.example.songsong.client;

import java.io.*;
import java.rmi.Naming;
import java.util.List;
import java.util.Random;
import com.example.songsong.config.DirectoryInterface;
import com.example.songsong.impl.DaemonImpl;

public class ClientConfig {
    private static final int FILE_PORT = 5000;

    public static void main(String[] args) {
        try {
            if (args.length < 4) {
                System.err.println("Usage: java com.example.songsong.client.ClientConfig <clientId> <fileName> <mode> <sourcePath>");
                System.exit(1);
            }

            String clientId = args[0];      // ID của client
            String fileName = args[1];      // Tên file
            String mode = args[2];          // "send" hoặc "receive"
            String sourcePath = args[3];    // Đường dẫn file gốc (nếu là send)

            DirectoryInterface directory = (DirectoryInterface) Naming.lookup("rmi://localhost/DirectoryService");
            DaemonImpl daemon = new DaemonImpl(clientId);

            // Tạo thư mục cho client
            String clientFolderPath = "src/main/java/com/example/songsong/data/" + clientId + "/";
            new File(clientFolderPath).mkdirs();

            if ("send".equalsIgnoreCase(mode)) {
                // Copy file từ sourcePath vào folder client
                String destinationPath = clientFolderPath + fileName;
                copyFile(new File(sourcePath), new File(destinationPath));
                System.out.println(clientId + " has copied file to: " + destinationPath);

                // Bắt đầu gửi file
                System.out.println(clientId + " is sending file: " + fileName);
                new Thread(() -> ClientSender.startFileServer(destinationPath, FILE_PORT)).start();

            } else if ("receive".equalsIgnoreCase(mode)) {
                // Tìm client có file và tải xuống
                List<String> clientsWithFile = directory.getClientsWithFile(fileName);
                if (clientsWithFile.isEmpty()) {
                    System.out.println("No clients found with file: " + fileName);
                    return;
                }

                Random random = new Random();
                String targetClient = clientsWithFile.get(random.nextInt(clientsWithFile.size()));
                System.out.println(clientId + " is downloading from: " + targetClient);

                // Tải file về folder client
                String downloadPath = clientFolderPath + "downloaded_" + fileName;
                ClientReceiver.downloadFile("localhost", FILE_PORT, downloadPath);
            } else {
                System.err.println("Invalid mode. Use 'send' or 'receive'.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Phương thức sao chép file
    private static void copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}

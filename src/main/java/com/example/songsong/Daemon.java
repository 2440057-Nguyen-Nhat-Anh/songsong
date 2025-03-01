package com.example.songsong;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Daemon {
    private static final String DIR_HOST = "localhost";
    private static final int DIR_PORT_DEFAULT = 1099;
    private static final int DAEMON_PORT_BASE = 5001;
    private static final int DAEMON_PORT_MAX = 5999;
    private static final String CLIENT_ID_BASE = "Client-";

    public static void main(String[] args) {
        int dirPort = DIR_PORT_DEFAULT;
        if (args.length > 0) {
            try {
                dirPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid directory port: " + args[0] + ". Using default port " + DIR_PORT_DEFAULT);
            }
        }

        String folder = ".";
        if (args.length > 1) {
            folder = args[1];
            File dir = new File(folder);
            if (!dir.exists() && !dir.mkdir()) {
                System.err.println("Cannot create folder: " + folder);
                return;
            }
        }

        // Each client gets its own folder. No download request at startup.
        String fileToDownload = null;
        if (args.length > 2) {
            fileToDownload = args[2];
        }

        int daemonPort = findAvailablePort(DAEMON_PORT_BASE);
        if (daemonPort == -1) {
            System.err.println("No available ports found between " + DAEMON_PORT_BASE + " and " + DAEMON_PORT_MAX);
            return;
        }

        String clientId = CLIENT_ID_BASE + daemonPort;

        File dir = new File(folder);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Invalid folder: " + folder);
            return;
        }
        List<String> files = Arrays.stream(dir.listFiles())
                                   .filter(File::isFile)
                                   .map(File::getName)
                                   .collect(Collectors.toList());
        System.out.println(files.size() + " files found in " + folder);
        System.out.println("Scanning directory: " + dir.getAbsolutePath());

        // Set system properties so DownloadImpl can locate the Directory and to mark this client's ID.
        System.setProperty("directory.host", DIR_HOST);
        System.setProperty("directory.port", String.valueOf(dirPort));
        System.setProperty("local.client.id", clientId);

        try {
            ClientImpl client = new ClientImpl();
            client.setClientInfo(clientId, DIR_HOST, daemonPort, folder, files);
            client.registerDirectory(DIR_HOST, dirPort);
            client.startFileServer(daemonPort);
            client.sendNotice(DIR_HOST, dirPort);

            System.out.println("Daemon " + clientId + " started on port " + daemonPort +
                               " serving files from folder: " + folder +
                               " with Directory on port " + dirPort);

            // Now wait for user commands
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter a filename to download or type 'exit' to quit:");
                String input = scanner.nextLine().trim();
                if ("exit".equalsIgnoreCase(input)) {
                    System.out.println("Exiting client.");
                    break;
                }
                if (!input.isEmpty()) {
                    IDownload downloadService = new DownloadImpl();
                    downloadService.downloadFile(input);
                }
            }
            scanner.close();
        } catch (Exception e) {
            System.err.println("Failed to start Daemon: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int findAvailablePort(int basePort) {
        for (int port = basePort; port <= DAEMON_PORT_MAX; port++) {
            try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(port)) {
                return port;
            } catch (Exception e) {
                continue;
            }
        }
        return -1;
    }
}

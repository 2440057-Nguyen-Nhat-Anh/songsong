package com.example.songsong;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.net.*;

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
                System.err.println("Invalid port specified: " + args[0] + ". Using default port " + DIR_PORT_DEFAULT);
            }
        }

        String folder = ".";
        if (args.length > 1) {
            folder = args[1];
            File dir = new File(folder);
            if (!dir.exists() && dir.mkdir()) {
                System.out.println("Created new folder: " + folder);
            }
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

        if (files.isEmpty()) {
            System.err.println("No files in " + folder);
        }

        // Set system properties for DownloadImpl to use
        System.setProperty("directory.host", DIR_HOST);
        System.setProperty("directory.port", String.valueOf(dirPort));
        System.setProperty("local.daemon.port", String.valueOf(daemonPort));

        try {
            ClientImpl client = new ClientImpl();
            client.setClientInfo(clientId, DIR_HOST, daemonPort, folder, files);

            client.registerDirectory(DIR_HOST, dirPort);
            client.startFileServer(daemonPort);
            client.sendNotice(DIR_HOST, dirPort);
            System.out.println("Daemon " + clientId + " started on port " + daemonPort + 
                              " serving files from " + folder + " with Directory on port " + dirPort);
        } catch (Exception e) {
            System.err.println("Failed to start Daemon: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int findAvailablePort(int basePort){
        for (int port = basePort; port <= DAEMON_PORT_MAX; port++) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                // If no exception, the port is available
                return port;
            } catch (IOException e) {
                // Port is in use—try the next one
                continue;
            }
        }
        return -1; // No available port found
    }

    public static int getLocalDaemonPort() {
        String portStr = System.getProperty("local.daemon.port");
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid local daemon port in system property: " + portStr);
            }
        }
        return DAEMON_PORT_BASE; // Fallback to base port if no property set (unlikely after startup)
    }

}

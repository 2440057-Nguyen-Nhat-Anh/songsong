package com.example.songsong;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Daemon {
    private static final String DIR_HOST = "localhost";         // Directory IP (default)
    private static final int DIR_PORT_DEFAULT = 1099;           // Default Directory RMI port
    private static final int DAEMON_PORT = 5001;
    private static final String CLIENT_ID = "Client-" + DAEMON_PORT;

    public static void main(String[] args) {
        int dirPort = DIR_PORT_DEFAULT;
        if (args.length > 0) {
            try {
                dirPort = Integer.parseInt(args[0]); // First argument is DIR_PORT
            } catch (NumberFormatException e) {
                System.err.println("Invalid port specified: " + args[0] + ". Using default port " + DIR_PORT_DEFAULT);
            }
        }

        String folder = ".";
        if (args.length > 1) {
            folder = args[1]; // Second argument is directory name (optional)
            File dir = new File(folder);
            if (!dir.exists()) {
                if (dir.mkdir()) {
                    System.out.println("Created new folder: " + folder);
                } else {
                    System.err.println("Failed to create folder: " + folder);
                    return;
                }
            }
        }

        // Ensure the directory exists (working dir or custom)
        File dir = new File(folder);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Invalid folder: " + folder + ". Please specify a valid path.");
            return;
        }

        List<String> files = Arrays.stream(dir.listFiles())
                                .filter(File::isFile)
                                .map(File::getName)
                                .collect(Collectors.toList());

        if (files.isEmpty()) {
            System.err.println("No files in " + folder);
            return;
        }

        try {
            // Create and initialize ClientImpl as a Daemon with dynamic DIR_PORT
            ClientImpl client = new ClientImpl();
            client.setClientInfo(CLIENT_ID, DIR_HOST, DAEMON_PORT, folder, files);
            client.registerDirectory(DIR_HOST, dirPort); // Use dynamic port for registration
            client.startFileServer(DAEMON_PORT);                    // Start TCP file server on fixed port
            client.sendNotice(DIR_HOST, dirPort);        // Use dynamic port for heartbeats
            System.out.println("Daemon " + CLIENT_ID + " started on port " + DAEMON_PORT + 
                            " serving files from " + folder + " with Directory on port " + dirPort);
        } catch (Exception e) {
            System.err.println("Failed to start Daemon: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

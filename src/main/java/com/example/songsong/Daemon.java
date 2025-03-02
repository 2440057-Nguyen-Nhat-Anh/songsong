package com.example.songsong;

import java.io.File;
import java.util.ArrayList;
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

        // Optionally, a filename can be passed as third argument for immediate download.
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

        // Set system properties so DownloadImpl can locate the Directory and mark this client's ID.
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

            // If a file is specified at startup, download it immediately.
            if (fileToDownload != null) {
                System.out.println("Attempting immediate download for file: " + fileToDownload);
                IDownload downloadService = new DownloadImpl();
                try {
                    downloadService.downloadFile(fileToDownload);
                    // Only update file list after successful download
                    updateFileList(client, clientId, DIR_HOST, daemonPort, folder, dirPort);
                } catch (Exception e) {
                    System.err.println("Error during immediate download: " + e.getMessage());
                }
            }

            // Interactive prompt for choosing download method:
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\nEnter download option and filename (format: <option> <filename> or 'exit' to quit):");
                System.out.println("  1: Parallel download");
                System.out.println("  2: Sequential download");
                System.out.println("  3: Sequential-all download");
                String line = scanner.nextLine().trim();
                if ("exit".equalsIgnoreCase(line)) {
                    System.out.println("Exiting client.");
                    break;
                }
                String[] parts = line.split("\\s+", 2);
                if (parts.length < 2) {
                    System.out.println("Please enter both an option and a filename.");
                    continue;
                }
                int option;
                try {
                    option = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid option.");
                    continue;
                }
                String fileName = parts[1].trim();
                IDownload downloadService = new DownloadImpl();
                
                boolean downloadSuccessful = false;
                try {
                    switch(option) {
                        case 1:
                            downloadService.downloadFile(fileName);
                            downloadSuccessful = true;
                            break;
                        case 2:
                            downloadService.downloadSequential(fileName);
                            downloadSuccessful = true;
                            break;
                        case 3:
                            downloadService.downloadSequentialAll(fileName);
                            downloadSuccessful = true;
                            break;
                        default:
                            System.out.println("Invalid option selected. Please choose 1, 2, or 3.");
                    }
                } catch (Exception e) {
                    System.err.println("Download failed: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Only update file list if download was successful
                if (downloadSuccessful) {
                    updateFileList(client, clientId, DIR_HOST, daemonPort, folder, dirPort);
                }
            }
            scanner.close();
        } catch (Exception e) {
            System.err.println("Failed to start Daemon: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Extract the file list update logic to a separate method
    private static void updateFileList(ClientImpl client, String clientId, String dirHost, 
                                      int daemonPort, String folder, int dirPort) {
        try {
            // Give the file system a moment to finalize writing
            Thread.sleep(100);
            
            // Scan for updated files
            List<String> updatedFiles = scanFiles(folder);
            
            // Update client information
            client.setClientInfo(clientId, dirHost, daemonPort, folder, updatedFiles);
            
            // Re-register to update the Directory with the new file list
            client.registerDirectory(dirHost, dirPort);
            
            System.out.println("Local file list updated after download. Files: " + updatedFiles);
        } catch (Exception e) {
            System.err.println("Failed to update file list: " + e.getMessage());
        }
    }

    // Help see all files after download
    public static List<String> scanFiles(String folder) {
        File dir = new File(folder);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dir.listFiles())
                    .filter(File::isFile)
                    .map(File::getName)
                    .collect(Collectors.toList());
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
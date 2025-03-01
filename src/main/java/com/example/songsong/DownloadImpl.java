package com.example.songsong;

import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.rmi.RemoteException;

public class DownloadImpl implements IDownload {
    private static final long FRAGMENT_SIZE = 5 * 1024 * 1024;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1099;

    @Override
    public void downloadFile(String fileName) {
        try {
            String d_host = System.getProperty("directory.host", DEFAULT_HOST);
            int d_port = Integer.parseInt(System.getProperty("directory.port", String.valueOf(DEFAULT_PORT)));
            Registry registry = LocateRegistry.getRegistry(d_host, d_port);
            DirectoryService directory = (DirectoryService) registry.lookup("DirectoryService");

            // Get a list of clients that have the file
            List<IClient> clients = directory.getAvailableClients(fileName);
            String localHost = "localhost";
            // // alternative
            // String localHost = InetAddress.getLocalHost().getHostAddress();

            // Filter out the local Daemon
            int localDaemonPort = getLocalDaemonPort();
            List<IClient> remoteClients = new ArrayList<>();
            for (IClient client : clients) {
                if (client.getHost().equals(localHost) && client.getPort() == localDaemonPort && localDaemonPort != -1) {
                    System.out.println("Excluding local Daemon on " + localHost + ":" + localDaemonPort);
                    continue;
                }
                remoteClients.add(client);
            }

            if (remoteClients.isEmpty()) {
                System.out.println("No remote clients with file: " + fileName);
                return;
            }

            long fileSize = directory.getFileSize(fileName); 
            long numFragments = (long) Math.ceil((double) fileSize / FRAGMENT_SIZE);

            // Create a thread pool for parallel downloads
            ExecutorService executor = Executors.newFixedThreadPool(remoteClients.size());

            long start = System.currentTimeMillis(); // Start timing for comparison

            // List of futures for each fragment download
            List<Future<byte[]>> futures = new ArrayList<>();

            // Round-robin assignment of fragments to remoteClients
            /* Pseudocode
             * For each fragment index from 0 up to the total number of fragments:
             *    Calculate the starting position (offset) of the fragment.
             *    Calculate the size of the fragment, ensuring the last fragment doesn't exceed file size.
             *    -> Get the client to download the fragment from the list of remoteClients in a round-robin fashion.
             */
            for (int i = 0; i < numFragments; i++) {

                long offset = i * FRAGMENT_SIZE;
                long fragmentSize = Math.min(FRAGMENT_SIZE, fileSize - offset);
                final int index = i;

                futures.add(executor.submit(() -> {
                        // Get the client to download the fragment
                        IClient client = remoteClients.get(index % remoteClients.size()); // Round-robin assignment
                        directory.reportDownloadStart(client.getClientID()); // Implement the DirectoryImpl that has this method
                        byte[] data = downloadFragment(remoteClients, index % remoteClients.size(), fileName, offset, fragmentSize);
                        directory.reportDownloadEnd(client.getClientID()); // Implement the DirectoryImpl that has this method
                return data;
            }));
            }

            // Write all fragments to a file
            try (FileOutputStream fos = new FileOutputStream("downloaded_" + fileName)) {
                for (Future<byte[]> future : futures) {
                    byte[] fragment = future.get();
                    fos.write(fragment);
                }
            }
            long end = System.currentTimeMillis(); // End timing
            executor.shutdown();
            System.out.println("Parallel download time with " + remoteClients.size() + " clients: " + (end - start) + "ms");
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void downloadSequential(String fileName) {
        try {
            String d_host = System.getProperty("directory.host", DEFAULT_HOST);
            int d_port = Integer.parseInt(System.getProperty("directory.port", String.valueOf(DEFAULT_PORT)));
            Registry registry = LocateRegistry.getRegistry(d_host, d_port);
            DirectoryService directory = (DirectoryService) registry.lookup("DirectoryService");

            // Get a list of clients that have the file
            List<IClient> clients = directory.getAvailableClients(fileName);
            String localHost = "localhost";
            // // alternative
            // String localHost = InetAddress.getLocalHost().getHostAddress();

            // Filter out the local Daemon
            int localDaemonPort = getLocalDaemonPort();
            List<IClient> remoteClients = new ArrayList<>();
            for (IClient client : clients) {
                if (client.getHost().equals(localHost) && client.getPort() == localDaemonPort && localDaemonPort != -1) {
                    System.out.println("Excluding local Daemon on " + localHost + ":" + localDaemonPort);
                    continue;
                }
                remoteClients.add(client);
            }

            if (remoteClients.isEmpty()) {
                System.out.println("No remote clients with file: " + fileName);
                return;
            }

            IClient client = remoteClients.get(0); 
            long fileSize = directory.getFileSize(fileName);

            long start = System.currentTimeMillis(); // Start timing
            directory.reportDownloadStart(client.getClientID());
            byte[] data = downloadFragment(remoteClients, 0, fileName, 0, fileSize);
            directory.reportDownloadEnd(client.getClientID());
            try (FileOutputStream fos = new FileOutputStream("sequential_" + fileName)) {
                fos.write(data); // Write entire file
            }
            long end = System.currentTimeMillis(); // End timing
            System.out.println("Sequential download time: " + (end - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void downloadSequentialAll(String fileName) {
        try {
            String d_host = System.getProperty("directory.host", DEFAULT_HOST);
            int d_port = Integer.parseInt(System.getProperty("directory.port", String.valueOf(DEFAULT_PORT)));
            Registry registry = LocateRegistry.getRegistry(d_host, d_port);
            DirectoryService directory = (DirectoryService) registry.lookup("DirectoryService");

            // Get a list of clients that have the file
            List<IClient> clients = directory.getAvailableClients(fileName);
            String localHost = "localhost";
            // // alternative
            // String localHost = InetAddress.getLocalHost().getHostAddress();

            // Filter out the local Daemon
            int localDaemonPort = getLocalDaemonPort();
            List<IClient> remoteClients = new ArrayList<>();
            for (IClient client : clients) {
                if (client.getHost().equals(localHost) && client.getPort() == localDaemonPort && localDaemonPort != -1) {
                    System.out.println("Excluding local Daemon on " + localHost + ":" + localDaemonPort);
                    continue;
                }
                remoteClients.add(client);
            }

            if (remoteClients.isEmpty()) {
                System.out.println("No remote clients with file: " + fileName);
                return;
            }

            long fileSize = directory.getFileSize(fileName);
            long numFragments = (long) Math.ceil((double) fileSize / FRAGMENT_SIZE);

            long start = System.currentTimeMillis(); // Start timing

            try (FileOutputStream fos = new FileOutputStream("sequential_all_" + fileName)) {
                // Sequentially download fragments, cycling through remote clients
                for (int i = 0; i < numFragments; i++) {
                    long offset = i * FRAGMENT_SIZE;
                    long fragmentSize = Math.min(FRAGMENT_SIZE, fileSize - offset);
                    IClient client = remoteClients.get(i % remoteClients.size()); // Round-robin client selection
                    directory.reportDownloadStart(client.getClientID());
                    byte[] data = downloadFragment(remoteClients, i % remoteClients.size(), fileName, offset, fragmentSize);
                    directory.reportDownloadEnd(client.getClientID());
                    if (data.length > 0) {
                        fos.write(data); // Write fragment to file
                    } else {
                        System.err.println("Fragment " + i + " failed to download from all clients");
                    }
                }
            }
            long end = System.currentTimeMillis(); // End timing
            System.out.println("Sequential-all download time with " + remoteClients.size() + " clients: " + (end - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] downloadFragment(List<IClient> clients, int clientIndex, String fileName, long offset, long fragmentSize) {
        // Fault-tolerant download of a fragment from a client
        // Iterate through clients starting from the assigned one
        for (int i = clientIndex; i < clients.size(); i++) { 
        IClient client = clients.get(i % clients.size()); // Select client in round-robin fashion
        try (Socket socket = new Socket(client.getHost(), client.getPort()); // Connect to the client’s TCP server
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // Send the request for the fragment
            dos.writeUTF(fileName); 
            dos.writeLong(offset);  
            dos.writeLong(fragmentSize);    // Size of the fragment
            dos.flush();

            // Read the response from the client
            int bytesRead = dis.readInt(); // Length of the compressed data
            if (bytesRead == -1) throw new IOException("File not found"); // Client indicates file not available
            byte[] compressed = new byte[bytesRead];
            dis.readFully(compressed); // Read the compressed fragment data
            try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
                    GZIPInputStream gzip = new GZIPInputStream(bais)) {
                return gzip.readAllBytes(); // Decompress and return the fragment
                }
            } 
            catch (IOException e) { 
                try {
                    System.out.println("Client " + client.getClientID() + " failed, proceeding to next client");
                } catch (RemoteException re) {
                    System.out.println("A remote error occurred while retrieving Client ID. Proceeding to next client");
                }
            }
        }

    return new byte[0];
    }

    private int getLocalDaemonPort() {
        // Try to get the port from the system property set by Daemon
        String portStr = System.getProperty("local.daemon.port");
        System.out.println("Local Daemon port: " + portStr);
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid local daemon port in system property: " + portStr);
            }
        }

        // Fallback: Try to access Daemon.getLocalDaemonPort() if available in the same JVM
        try {
            return Daemon.getLocalDaemonPort(); // Use static method if Daemon is co-located
        } catch (Exception e) {
            System.err.println("Could not access Daemon port directly: " + e.getMessage());
        }
        System.out.println("No local Daemon port found");
        return -1;
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java com.example.songsong.DownloadImpl <option> <fileName>");
            System.err.println("Options:");
            System.err.println("  1: Parallel download");
            System.err.println("  2: Sequential download");
            System.err.println("  3: Sequential-all download");
            System.err.println("Example: java com.example.songsong.DownloadImpl 1 largefile1.zip");
            System.exit(1);
        }

        int option;
        try {
            option = Integer.parseInt(args[0]);
            if (option < 1 || option > 3) {
                System.err.println("Invalid option. Use 1, 2, or 3.");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid input for option. Use 1, 2, or 3.");
            return;
        }

        String fileName = args[1];
        IDownload downloadService = new DownloadImpl();
        switch (option) {
            case 1: downloadService.downloadFile(fileName); break;
            case 2: downloadService.downloadSequential(fileName); break;
            case 3: downloadService.downloadSequentialAll(fileName); break;
        }
    }
}


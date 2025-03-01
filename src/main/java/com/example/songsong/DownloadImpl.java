package com.example.songsong;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

public class DownloadImpl implements IDownload {
    // request 5MB at a time
    private static final long FRAGMENT_SIZE = 5 * 1024 * 1024;

    @Override
    public void downloadFile(String d_host, int d_port, String fileName) {
        try {
            Registry registry = LocateRegistry.getRegistry(d_host, d_port);
            // Implement the DirectoryImpl class
            DirectoryImpl directory = (DirectoryImpl) registry.lookup("DirectoryService");

            // Get a list of clients that have the file
            List<ClientImpl> clients = directory.getAvailableClients(fileName); // Implement the DirectoryImpl that has this method (remeber not to consider the client that is downloading the file)
            if (clients.isEmpty()) {
                System.out.println("No clients with file: " + fileName);
                return;
            }
            // File size and number of fragments
            long fileSize = directory.getFileSize(fileName); // Implement the DirectoryImpl that has this method
            long numFragments = (long) Math.ceil((double) fileSize / FRAGMENT_SIZE);

            // Create a thread pool for parallel downloads
            ExecutorService executor = Executors.newFixedThreadPool(clients.size());

            long start = System.currentTimeMillis(); // Start timing for comparison

            // List of futures for each fragment download
            List<Future<byte[]>> futures = new ArrayList<>();

            // Round-robin assignment of fragments to clients
            /* Pseudocode
             * For each fragment index from 0 up to the total number of fragments:
             *    Calculate the starting position (offset) of the fragment.
             *    Calculate the size of the fragment, ensuring the last fragment doesn't exceed file size.
             *    -> Get the client to download the fragment from the list of clients in a round-robin fashion.
             */
            for (int i = 0; i < numFragments; i++) {

                long offset = i * FRAGMENT_SIZE;
                long fragmentSize = Math.min(FRAGMENT_SIZE, fileSize - offset);
                final int index = i;

                futures.add(executor.submit(() -> {
                        // Get the client to download the fragment
                        ClientImpl client = clients.get(index % clients.size()); // Round-robin assignment
                        directory.reportDownloadStart(client.getClientID()); // Implement the DirectoryImpl that has this method
                        byte[] data = downloadFragment(clients, index % clients.size(), fileName, offset, fragmentSize);
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
            System.out.println("Parallel download time with " + clients.size() + " clients: " + (end - start) + "ms");
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void downloadSequential(String d_host, int d_port, String fileName) {
        try {
            Registry registry = LocateRegistry.getRegistry(d_host, d_port);

            // Implement the DirectoryImpl class and replace the DirectoryServiceInt with the actual name of the class
            DirectoryImpl directory = (DirectoryImpl) registry.lookup("DirectoryServiceInt");

            List<ClientImpl> clients = directory.getAvailableClients(fileName); // Implement the DirectoryImpl that has this method
            if (clients.isEmpty()) return;

            // Use the first available client for sequential download
            ClientImpl client = clients.get(0);

            // Get filesize
            long fileSize = directory.getFileSize(fileName);

            long start = System.currentTimeMillis(); // Start timing for comparison
            directory.reportDownloadStart(client.getClientID()); // Implement the DirectoryImpl that has this method
            byte[] data = downloadFragment(clients, 0, fileName, 0, fileSize);
            directory.reportDownloadEnd(client.getClientID()); // Implement the DirectoryImpl that has this method
            try (FileOutputStream fos = new FileOutputStream("sequential_" + fileName)) {
                fos.write(data);
            }
            long end = System.currentTimeMillis(); // End timing
            System.out.println("Sequential download time: " + (end - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void downloadSequentialAll(String d_host, int d_port, String fileName) {
        try {
            Registry registry = LocateRegistry.getRegistry(d_host, d_port);
            DirectoryImpl directory = (DirectoryImpl) registry.lookup("DirectoryServiceInt");
            List<ClientImpl> clients = directory.getAvailableClients(fileName); // Match downloadFile
            if (clients.isEmpty()) {
                System.out.println("No clients with file: " + fileName);
                return;
            }

            long fileSize = directory.getFileSize(fileName); // Match downloadFile
            long numFragments = (long) Math.ceil((double) fileSize / FRAGMENT_SIZE); // Match downloadFile

            long start = System.currentTimeMillis(); // Start timing for comparison

            try (FileOutputStream fos = new FileOutputStream("sequential_all_" + fileName)) {
                // Round-robin assignment of fragments to clients, sequentially
                for (int i = 0; i < numFragments; i++) {
                    long offset = i * FRAGMENT_SIZE; // Calculate starting position
                    long fragmentSize = Math.min(FRAGMENT_SIZE, fileSize - offset); // Calculate fragment size
                    ClientImpl client = clients.get(i % clients.size()); // Round-robin client selection
                    directory.reportDownloadStart(client.getClientID()); // Report start
                    byte[] data = downloadFragment(clients, i % clients.size(), fileName, offset, fragmentSize); // Download fragment
                    directory.reportDownloadEnd(client.getClientID()); // Report end
                    if (data.length > 0) { // Check for successful download
                        fos.write(data); // Write fragment to file
                    } else {
                        System.err.println("Fragment " + i + " failed to download from all clients");
                    }
                }
            }
            long end = System.currentTimeMillis(); // End timing
            System.out.println("Sequential-all download time with " + clients.size() + " clients: " + (end - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] downloadFragment(List<ClientImpl> clients, int clientIndex, String fileName, long offset, long fragmentSize) {
        // Fault-tolerant download of a fragment from a client
        // Iterate through clients starting from the assigned one
        for (int i = clientIndex; i < clients.size(); i++) { 
        ClientImpl client = clients.get(i % clients.size()); // Select client in round-robin fashion
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
                System.out.println("Client " + client.getClientID() + " failed, proceeding to next client");
            }
        }
    return new byte[0];
    }


}


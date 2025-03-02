package com.example.songsong;

import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

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

            // Filter out local or unreachable clients
            List<IClient> clients = directory.getAvailableClients(fileName);
            String localClientId = System.getProperty("local.client.id", "");
            List<IClient> remoteClients = new ArrayList<>();
            for (IClient client : clients) {
                try {
                    if (!client.getClientID().equals(localClientId)) {
                        remoteClients.add(client);
                    } else {
                        System.out.println("Excluding local client " + localClientId + " from download.");
                    }
                } catch (RemoteException e) {
                    System.out.println("Skipping client due to connection error: " + e.getMessage());
                }
            }

            if (remoteClients.isEmpty()) {
                System.out.println("No remote clients with file: " + fileName);
                return;
            }

            long fileSize = directory.getFileSize(fileName);
            long numFragments = (long) Math.ceil((double) fileSize / FRAGMENT_SIZE);
            ExecutorService executor = Executors.newFixedThreadPool(remoteClients.size());
            long start = System.currentTimeMillis();
            List<Future<byte[]>> futures = new ArrayList<>();

            for (int i = 0; i < numFragments; i++) {
                final int fragmentIndex = i;
                long offset = i * FRAGMENT_SIZE;
                long fragmentSize = Math.min(FRAGMENT_SIZE, fileSize - offset);
                System.out.println("Downloading fragment " + fragmentIndex + " (offset: " + offset + ", size: " + fragmentSize + ")");
                Future<byte[]> future = executor.submit(() -> {
                    int attempts = 0;
                    IOException lastException = null;
                    // Try each client in turn for this fragment
                    while (attempts < remoteClients.size()) {
                        IClient client = remoteClients.get((fragmentIndex + attempts) % remoteClients.size());
                        String clientId;
                        try {
                            clientId = client.getClientID();
                        } catch (RemoteException re) {
                            System.out.println("RemoteException retrieving client ID: " + re.getMessage() + "; trying next client.");
                            attempts++;
                            continue;
                        }
                        try {
                            directory.reportDownloadStart(clientId);
                        } catch (RemoteException re) {
                            System.out.println("RemoteException reporting download start for client " + clientId + ": " + re.getMessage());
                            attempts++;
                            continue;
                        }
                        try {
                            return attemptFragmentDownload(client, fileName, offset, fragmentSize);
                        } catch (IOException e) {
                            lastException = e;
                            System.out.println("Failed to download fragment " + fragmentIndex + " from client " + clientId + "; trying next client.");
                        } finally {
                            try {
                                directory.reportDownloadEnd(clientId);
                            } catch (RemoteException re) {
                                System.out.println("RemoteException reporting download end for client " + clientId + ": " + re.getMessage());
                            }
                        }
                        attempts++;
                    }
                    throw new IOException("Failed to download fragment " + fragmentIndex + " from all clients.", lastException);
                });
                futures.add(future);
            }

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                for (Future<byte[]> future : futures) {
                    byte[] fragment = future.get();
                    fos.write(fragment);
                }
            }
            executor.shutdown();
            long end = System.currentTimeMillis();
            System.out.println("Parallel download time with " + remoteClients.size() + " clients: " + (end - start) + " ms");
        } catch (Exception e) {
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

            List<IClient> clients = directory.getAvailableClients(fileName);
            String localClientId = System.getProperty("local.client.id", "");
            List<IClient> remoteClients = new ArrayList<>();
            for (IClient client : clients) {
                try {
                    if (!client.getClientID().equals(localClientId)) {
                        remoteClients.add(client);
                    }
                } catch (RemoteException e) {
                    System.out.println("Skipping client due to connection error: " + e.getMessage());
                }
            }

            if (remoteClients.isEmpty()) {
                System.out.println("No remote clients with file: " + fileName);
                return;
            }

            IClient client = remoteClients.get(0);
            long fileSize = directory.getFileSize(fileName);
            long start = System.currentTimeMillis();
            String clientId = client.getClientID();
            directory.reportDownloadStart(clientId);
            try {
                byte[] data = attemptFragmentDownload(client, fileName, 0, fileSize);
                try (FileOutputStream fos = new FileOutputStream(fileName)) {
                    fos.write(data);
                }
            } finally {
                directory.reportDownloadEnd(clientId);
            }
            long end = System.currentTimeMillis();
            System.out.println("Sequential download time: " + (end - start) + " ms");
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

            List<IClient> clients = directory.getAvailableClients(fileName);
            String localClientId = System.getProperty("local.client.id", "");
            List<IClient> remoteClients = new ArrayList<>();
            for (IClient c : clients) {
                try {
                    if (!c.getClientID().equals(localClientId)) {
                        remoteClients.add(c);
                    }
                } catch (RemoteException e) {
                    System.out.println("Skipping client due to connection error: " + e.getMessage());
                }
            }

            if (remoteClients.isEmpty()) {
                System.out.println("No remote clients with file: " + fileName);
                return;
            }

            long fileSize = directory.getFileSize(fileName);
            long numFragments = (long) Math.ceil((double) fileSize / FRAGMENT_SIZE);
            long start = System.currentTimeMillis();

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                for (int i = 0; i < numFragments; i++) {
                    long offset = i * FRAGMENT_SIZE;
                    long fragmentSize = Math.min(FRAGMENT_SIZE, fileSize - offset);
                    IClient client = remoteClients.get(i % remoteClients.size());
                    String clientId = client.getClientID();
                    directory.reportDownloadStart(clientId);
                    try {
                        byte[] data = attemptFragmentDownload(client, fileName, offset, fragmentSize);
                        if (data.length == 0) {
                            System.err.println("Fragment " + i + " failed from all clients.");
                        } else {
                            fos.write(data);
                        }
                    } catch (IOException e) {
                        System.err.println("Fragment " + i + " failed: " + e.getMessage());
                    } finally {
                        directory.reportDownloadEnd(clientId);
                    }
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("Sequential-all download time with " + remoteClients.size() + " clients: " + (end - start) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to download a fragment from a single client.
    private byte[] attemptFragmentDownload(IClient client, String fileName, long offset, long fragmentSize) throws IOException {
        try (Socket socket = new Socket(client.getHost(), client.getPort());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("GET_FRAGMENT");
            dos.writeUTF(fileName);
            dos.writeLong(offset);
            dos.writeLong(fragmentSize);
            dos.flush();

            int compressedLength = dis.readInt();
            if (compressedLength == -1) {
                throw new IOException("Fragment not available from client " + client.getClientID());
            }
            byte[] compressed = new byte[compressedLength];
            dis.readFully(compressed);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
                 GZIPInputStream gzip = new GZIPInputStream(bais)) {
                System.out.println("Downloaded fragment (" + offset + " to " + (offset + fragmentSize) +
                                   ") from client " + client.getClientID());
                return gzip.readAllBytes();
            }
        }
    }
}

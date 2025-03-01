package com.example.songsong;

import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
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

            List<IClient> clients = directory.getAvailableClients(fileName);
            String localClientId = System.getProperty("local.client.id", "");
            List<IClient> remoteClients = new ArrayList<>();
            for (IClient client : clients) {
                if (!client.getClientID().equals(localClientId)) {
                    remoteClients.add(client);
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
                IClient assignedClient = remoteClients.get(i % remoteClients.size());
                directory.reportDownloadStart(assignedClient.getClientID());

                Future<byte[]> future = executor.submit(() -> {
                    try {
                        return downloadFragment(remoteClients, fragmentIndex % remoteClients.size(), fileName, offset, fragmentSize);
                    } finally {
                        directory.reportDownloadEnd(assignedClient.getClientID());
                    }
                });
                futures.add(future);
            }

            try (FileOutputStream fos = new FileOutputStream("downloaded_" + fileName)) {
                for (Future<byte[]> future : futures) {
                    fos.write(future.get());
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
                if (!client.getClientID().equals(localClientId)) {
                    remoteClients.add(client);
                }
            }

            if (remoteClients.isEmpty()) {
                System.out.println("No remote clients with file: " + fileName);
                return;
            }

            IClient client = remoteClients.get(0);
            long fileSize = directory.getFileSize(fileName);
            long start = System.currentTimeMillis();
            directory.reportDownloadStart(client.getClientID());
            try {
                byte[] data = downloadFragment(remoteClients, 0, fileName, 0, fileSize);
                try (FileOutputStream fos = new FileOutputStream("sequential_" + fileName)) {
                    fos.write(data);
                }
            } finally {
                directory.reportDownloadEnd(client.getClientID());
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
                if (!c.getClientID().equals(localClientId)) {
                    remoteClients.add(c);
                }
            }

            if (remoteClients.isEmpty()) {
                System.out.println("No remote clients with file: " + fileName);
                return;
            }

            long fileSize = directory.getFileSize(fileName);
            long numFragments = (long) Math.ceil((double) fileSize / FRAGMENT_SIZE);
            long start = System.currentTimeMillis();

            try (FileOutputStream fos = new FileOutputStream("sequential_all_" + fileName)) {
                for (int i = 0; i < numFragments; i++) {
                    long offset = i * FRAGMENT_SIZE;
                    long fragmentSize = Math.min(FRAGMENT_SIZE, fileSize - offset);
                    IClient client = remoteClients.get(i % remoteClients.size());
                    directory.reportDownloadStart(client.getClientID());
                    try {
                        byte[] data = downloadFragment(remoteClients, i % remoteClients.size(), fileName, offset, fragmentSize);
                        if (data.length == 0) {
                            System.err.println("Fragment " + i + " failed from all clients.");
                        } else {
                            fos.write(data);
                        }
                    } finally {
                        directory.reportDownloadEnd(client.getClientID());
                    }
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("Sequential-all download time with " + remoteClients.size() + " clients: " + (end - start) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] downloadFragment(List<IClient> clients, int clientIndex, String fileName, long offset, long fragmentSize) {
        int totalClients = clients.size();
        for (int i = 0; i < totalClients; i++) {
            IClient client = clients.get((clientIndex + i) % totalClients);
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
                    throw new IOException("Server indicates fragment not available.");
                }
                byte[] compressed = new byte[compressedLength];
                dis.readFully(compressed);

                try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
                     GZIPInputStream gzip = new GZIPInputStream(bais)) {
                    return gzip.readAllBytes();
                }
            } catch (IOException e) {
                try {
                    System.out.println("Failed to download fragment from " + client.getClientID() + "; trying next client.");
                } catch (RemoteException re) {
                    System.out.println("Failed to get client ID; trying next client.");
                }
            }
        }
        return new byte[0];
    }
}

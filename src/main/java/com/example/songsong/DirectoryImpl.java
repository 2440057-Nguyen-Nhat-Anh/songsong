package com.example.songsong;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DirectoryImpl extends UnicastRemoteObject implements DirectoryService {
    // Maps file names to lists of clients that have the file
    private Map<String, List<IClient>> fileToClients = new ConcurrentHashMap<>();
    // Tracks the last heartbeat timestamp for each client
    private Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();
    // Tracks the number of active downloads per client for load balancing
    private Map<String, Integer> clientLoad = new ConcurrentHashMap<>();
    // Timeout (in milliseconds) after which a client is considered disconnected
    private static final long TIMEOUT = 60000; // 1 minute

    public DirectoryImpl() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerClient(IClient client) throws RemoteException {
        String clientId = client.getClientID();
        lastHeartbeat.put(clientId, System.currentTimeMillis());
        for (String file : client.getFiles()) {
            fileToClients.computeIfAbsent(file, k -> new ArrayList<>()).add(client);
        }
        System.out.println("Registered client: " + clientId + " with files: " + client.getFiles());
    }

    @Override
    public List<IClient> getAvailableClients(String fileName) throws RemoteException {
        List<IClient> clients = fileToClients.getOrDefault(fileName, Collections.emptyList());
        // Remove clients that have timed out or are overloaded
        clients.removeIf(client -> {
            try {
                Long last = lastHeartbeat.get(client.getClientID());
                boolean timeout = (last == null) || ((System.currentTimeMillis() - last) > TIMEOUT);
                boolean overload = clientLoad.getOrDefault(client.getClientID(), 0) > 5;
                return timeout || overload;
            } catch (RemoteException e) {
                e.printStackTrace();
                return true;
            }
        });
        return clients;
    }

    @Override
    public void heartbeat(String clientId) throws RemoteException {
        lastHeartbeat.put(clientId, System.currentTimeMillis());
    }

    @Override
    public void reportDownloadStart(String clientId) throws RemoteException {
        clientLoad.merge(clientId, 1, Integer::sum);
    }

    @Override
    public void reportDownloadEnd(String clientId) throws RemoteException {
        clientLoad.computeIfPresent(clientId, (k, v) -> Math.max(0, v - 1));
    }

    @Override
    public long getFileSize(String fileName) throws RemoteException {
        List<IClient> clients = fileToClients.getOrDefault(fileName, Collections.emptyList());
        if (clients.isEmpty()) {
            throw new RemoteException("No such file found: " + fileName);
        }
        IClient client = clients.get(0); // use the first available client
        try (Socket socket = new Socket(client.getHost(), client.getPort());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("GET_SIZE");
            dos.writeUTF(fileName);
            dos.flush();

            long size = dis.readLong();
            if (size < 0) {
                throw new RemoteException("Client reports file does not exist: " + fileName);
            }
            return size;
        } catch (IOException e) {
            throw new RemoteException("Failed to get file size from client " + client.getClientID(), e);
        }
    }

    @Override
    public void startHeartbeatMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                long currentTime = System.currentTimeMillis();
                for (Map.Entry<String, Long> entry : lastHeartbeat.entrySet()) {
                    String clientId = entry.getKey();
                    long lastTime = entry.getValue();
                    long elapsed = currentTime - lastTime;
                    if (elapsed > TIMEOUT) {
                        System.out.println("Client " + clientId + " disconnected (last heartbeat " + elapsed + " ms ago).");
                    } else {
                        System.out.println("Heartbeat received from client " + clientId + " (" + elapsed + " ms ago).");
                    }
                }
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
}

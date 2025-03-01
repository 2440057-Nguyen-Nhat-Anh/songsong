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
    private Map<String, List<IClient>> fileToClients = new ConcurrentHashMap<>();
    private Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();
    private Map<String, Integer> clientLoad = new ConcurrentHashMap<>();
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
        clients.removeIf(client -> {
            try {
                Long last = lastHeartbeat.get(client.getClientID());
                return last == null || (System.currentTimeMillis() - last) > TIMEOUT
                       || clientLoad.getOrDefault(client.getClientID(), 0) > 5;
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
        if (clients.isEmpty()) throw new RemoteException("No such file found: " + fileName);

        IClient client = clients.get(0);
        try (Socket socket = new Socket(client.getHost(), client.getPort());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("GET_SIZE");
            dos.writeUTF(fileName);
            dos.flush();
            long size = dis.readLong();
            if (size < 0) throw new RemoteException("Client reports file does not exist: " + fileName);
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
                    long elapsed = currentTime - entry.getValue();
                    if (elapsed > TIMEOUT) {
                        System.out.println("Client " + clientId + " disconnected (no heartbeat in " + elapsed + " ms).");
                    }
                }
                try {
                    Thread.sleep(5000);
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

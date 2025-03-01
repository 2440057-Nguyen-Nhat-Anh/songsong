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
    private Map<String, List<ClientImpl>> fileToClients = new ConcurrentHashMap<>();
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
    public synchronized void registerClient(ClientImpl client) throws RemoteException{
        String clientId = client.getClientID();
        lastHeartbeat.put(clientId, System.currentTimeMillis()); // Update the last heartbeat timestamp
        for (String file : client.getFiles()) {
            fileToClients.computeIfAbsent(file, k -> new ArrayList<>()).add(client);
        }
        System.out.println("Registered client: " + clientId + " with files: " + client.getFiles());
    }

    @Override
    public List<ClientImpl> getAvailableClients(String fileName) throws RemoteException {
        // Get the list of clients that have the file
        List<ClientImpl> clients = fileToClients.getOrDefault(fileName, Collections.emptyList());
        clients.removeIf(client -> {
            Long last = lastHeartbeat.get(client.getClientID());
            return last == null || (System.currentTimeMillis() - last) > TIMEOUT || 
                   clientLoad.getOrDefault(client.getClientID(), 0) > 0;
        });
        return clients;
    }

    @Override
    public void heartbeat(String clientId) throws RemoteException {
        // Update heartbeat timestamp to keep client alive
        lastHeartbeat.put(clientId, System.currentTimeMillis());
    }

    @Override
    public void reportDownloadStart(String clientId) throws RemoteException {
        // Increment load count for the client starting a download
        clientLoad.merge(clientId, 1, Integer::sum);
    }

    @Override
    public void reportDownloadEnd(String clientId) throws RemoteException {
        // Decrement load count for the client finishing a download
        clientLoad.computeIfPresent(clientId, (k, v) -> Math.max(0, v - 1));
    }    

    @Override
    public long getFileSize(String fileName) throws RemoteException {
        List<ClientImpl> clients = fileToClients.getOrDefault(fileName, Collections.emptyList());
        if (clients.isEmpty()) throw new RemoteException("No such file found: " + fileName);
        ClientImpl client = clients.get(0); // Use first available client to get file size
        try (Socket socket = new Socket(client.getHost(), client.getPort());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            dos.writeUTF("GET_SIZE");
            dos.writeUTF(fileName);
            dos.flush();
            return dis.readLong();
        } catch (IOException e) {
            throw new RemoteException("Failed to get file size", e);
        }
    }
    
}

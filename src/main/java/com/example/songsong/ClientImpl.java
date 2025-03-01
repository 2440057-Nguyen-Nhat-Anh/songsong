package com.example.songsong;

import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ClientImpl extends UnicastRemoteObject implements IClient {

    public ClientImpl() throws java.rmi.RemoteException {
        super();
    }

    protected class ClientInfo {
        String clientID;
        String host;
        int port;
        String clientFolder;
        List<String> Files; 
    }

    private ClientInfo clientInfo = new ClientInfo();

    // setter for clientInfo
    public void setClientInfo(String clientID, String host, int port, String clientFolder, List<String> files) {
        this.clientInfo.clientID = clientID;
        this.clientInfo.host = host;
        this.clientInfo.port = port;
        this.clientInfo.clientFolder = clientFolder;
        this.clientInfo.Files = files;
    }

    @Override
    public void registerDirectory(String d_host, int d_port) throws Exception {
        try {
            Registry registry = LocateRegistry.getRegistry(d_host, d_port);
            DirectoryService directory = (DirectoryService) registry.lookup("DirectoryService");
            // register with the directory server
            directory.registerClient(this);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void startFileServer(int port) throws Exception {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Client " + this.clientInfo.clientID + " is running on port " + port);

                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) 
                        {
    
                        String fileName = dis.readUTF();
                        long offset = dis.readLong();
                        long fragmentSize = dis.readLong();
    
                        File file = new File(fileName);
                        if (file.exists()) {
                            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                                raf.seek(offset);
                                byte[] buffer = new byte[(int) fragmentSize];
                                int bytesRead = raf.read(buffer);
                                gzip.write(buffer, 0, bytesRead);
                                gzip.finish();
                                byte[] compressed = baos.toByteArray();
                                dos.writeInt(compressed.length);
                                dos.write(compressed);
                                dos.flush();
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void sendNotice(String d_host, int d_port) throws RemoteException {
        new Thread(() -> {
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(d_host, d_port);
                    DirectoryService directory = (DirectoryService) registry.lookup("DirectoryService");
                    directory.heartbeat(this.clientInfo.clientID);
                    Thread.sleep(10000); // Every 10 seconds
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // getter for clientInfo
    public String getClientID() {
        return this.clientInfo.clientID;
    }

    // getter for host
    public String getHost() {
        return this.clientInfo.host;
    }

    // getter for port
    public int getPort() {
        return this.clientInfo.port;
    }

    // getter for clientFolder
    public String getClientFolder(){
        return this.clientInfo.clientFolder;
    }

    // getter for files
    public List<String> getFiles() {
        return this.clientInfo.Files;
    }
}


package com.example.songsong.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import com.example.songsong.config.DaemonInterface;

public class DaemonImpl extends UnicastRemoteObject implements DaemonInterface {
    private final String clientId;

    public DaemonImpl(String clientId) throws RemoteException {
        this.clientId = clientId;
    }

    @Override
    public String getClientId() throws RemoteException {
        return clientId;
    }

    @Override
    public byte[] downloadChunk(String fileName, int chunkSize, int chunkIndex) throws RemoteException {
        try {
            File file = new File(fileName);
            byte[] data = Files.readAllBytes(file.toPath());
            int start = chunkIndex * chunkSize;
            int end = Math.min(start + chunkSize, data.length);

            byte[] chunk = new byte[end - start];
            System.arraycopy(data, start, chunk, 0, chunk.length);
            return chunk;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}

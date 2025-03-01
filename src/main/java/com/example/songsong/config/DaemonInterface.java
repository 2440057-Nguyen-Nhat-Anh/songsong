package com.example.songsong.config;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DaemonInterface extends Remote {
    String getClientId() throws RemoteException;
    byte[] downloadChunk(String fileName, int chunkSize, int chunkIndex) throws RemoteException;
}

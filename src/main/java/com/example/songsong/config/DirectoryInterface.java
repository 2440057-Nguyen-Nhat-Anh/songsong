package com.example.songsong.config;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DirectoryInterface extends Remote {
    void registerFile(String fileName, String clientId) throws RemoteException;
    List<String> getClientsWithFile(String fileName) throws RemoteException;
}

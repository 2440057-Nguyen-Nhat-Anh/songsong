package com.example.songsong;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DirectoryService extends Remote {
    void registerClient(IClient client) throws RemoteException;
    List<IClient> getAvailableClients(String fileName) throws RemoteException;
    void heartbeat(String clientId) throws RemoteException;
    void reportDownloadStart(String clientId) throws RemoteException;
    void reportDownloadEnd(String clientId) throws RemoteException;
    long getFileSize(String fileName) throws RemoteException;
    void startHeartbeatMonitor() throws RemoteException;
}

package com.example.songsong;

import java.rmi.*;
import java.util.List;

public interface DirectoryService extends Remote {
    void registerClient(IClient client) throws RemoteException;
    List<IClient> getAvailableClients(String fileName) throws RemoteException; // Get all available clients for the file name
    void heartbeat(String clientId) throws RemoteException; // Updates the client's heartbeat to see if it is still alive
    // Notify Directory when a download starts from a Daemon
    void reportDownloadStart(String clientId) throws RemoteException;
    // Notify Directory when a download ends
    void reportDownloadEnd(String clientId) throws RemoteException;
    // Get the size of a file from a Daemon
    long getFileSize(String fileName) throws RemoteException;
    void startHeartbeatMonitor() throws RemoteException;
}

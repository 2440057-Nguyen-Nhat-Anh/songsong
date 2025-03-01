package com.example.songsong;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IClient extends Remote {
    void registerDirectory(String d_host, int d_port) throws RemoteException, Exception;
    void startFileServer(int port) throws RemoteException, Exception;
    void sendNotice(String d_host, int d_port) throws RemoteException;
    String getClientID() throws RemoteException;
    List<String> getFiles() throws RemoteException;
    String getHost() throws RemoteException;
    int getPort() throws RemoteException;
    String getClientFolder() throws RemoteException;
}
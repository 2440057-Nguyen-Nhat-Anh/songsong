package com.example.songsong.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import com.example.songsong.config.DirectoryInterface;

public class DirectoryImpl extends UnicastRemoteObject implements DirectoryInterface {
    private final Map<String, List<String>> fileRegistry = new HashMap<>();

    public DirectoryImpl() throws RemoteException {}

    @Override
    public synchronized void registerFile(String fileName, String clientId) throws RemoteException {
        fileRegistry.computeIfAbsent(fileName, k -> new ArrayList<>()).add(clientId);
        System.out.println(clientId + " registered: " + fileName);
    }

    @Override
    public synchronized List<String> getClientsWithFile(String fileName) throws RemoteException {
        return fileRegistry.getOrDefault(fileName, Collections.emptyList());
    }
}

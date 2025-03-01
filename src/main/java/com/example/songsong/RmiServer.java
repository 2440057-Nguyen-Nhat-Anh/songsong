package com.example.songsong;


import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

import com.example.songsong.config.DirectoryInterface;
import com.example.songsong.impl.DirectoryImpl;

public class RmiServer {
    public static void main(String[] args) {
        try {
            DirectoryInterface directory = new DirectoryImpl();
            LocateRegistry.createRegistry(1099);
            Naming.rebind("rmi://localhost/DirectoryService", directory);
            System.out.println("Directory Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

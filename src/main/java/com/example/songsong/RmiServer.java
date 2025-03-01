package com.example.songsong;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;

public class RmiServer {
    public static void main(String[] args) {

        int port = 1099;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try {
            DirectoryService directory = new DirectoryImpl();
            LocateRegistry.createRegistry(port);

            // Bind the directory to the RMI registry
            Naming.rebind("rmi://localhost/DirectoryService", directory);
            System.out.println("Directory Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

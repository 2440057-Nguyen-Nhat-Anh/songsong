package com.example.songsong;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class RmiServer {
    public static void main(String[] args) {
        int port = 1099;  // default RMI port
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try {
            DirectoryService directory = new DirectoryImpl();
            // Start RMI registry on the given port
            LocateRegistry.createRegistry(port);

            // Start the heartbeat monitor
            directory.startHeartbeatMonitor();

            // Bind the DirectoryService to the registry
            String bindUrl = "rmi://localhost:" + port + "/DirectoryService";
            Naming.rebind(bindUrl, directory);

            System.out.println("Directory Server is running at " + bindUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

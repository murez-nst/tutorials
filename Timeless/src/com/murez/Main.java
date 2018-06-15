package com.murez;

import com.murez.net.RemoteServer;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
/**
 * @author Murez Nasution
 */
public class Main {
    public static void main(String[] args) {
        String dbProperties = "db.properties";
        if(args != null) {
            if(args.length > 0) dbProperties = args[0];
        }
        System.setProperty("java.security.policy", "server.policy");
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
        if(System.getSecurityManager() == null)
            System.setSecurityManager(new SecurityManager());
        try {
            System.out.println("Loading...");
            launch(new RemoteServer(Paths.get(dbProperties), 0));
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    private static void launch(Remote remote) throws Exception {
        final Remote STUB;
        try {
            STUB = UnicastRemoteObject.exportObject(remote, 0);
        } catch(Throwable e) {
            throw new java.io.IOException("Failed of initializing Stub", e);
        }
        try {
            LocateRegistry.getRegistry().rebind("Authentication", STUB);
        } catch(Throwable e) {
            throw new UnsupportedOperationException("Binding Stub to RMI Registry was failed", e);
        }
        System.out.println("Server has started");
    }
}
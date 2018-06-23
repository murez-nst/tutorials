package com.murez;

import com.murez.util.Processor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
/**
 * @author Murez Nasution
 */
public class Main implements com.murez.sql.SQLMaintainable {
    private final static Function<Path, Boolean> FILE = file -> Files.exists(file) && Files.isRegularFile(file);
    private final static int MIN_LIMIT = 5;

    private final BlockingQueue<Processor> PROCESSORS;
    private final Object LOCK = new Object();
    private Path properties;

    public static void main(String[] args) {
        Path properties = Paths.get("timeless.properties");
        if(args != null) {
            if(args.length > 0) properties = Paths.get(args[0]);
        }
        System.setProperty("java.security.policy", "server.policy");
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
        if(System.getSecurityManager() == null)
            System.setSecurityManager(new SecurityManager());
        try {
            System.out.println("Loading...");
            new Main(properties);
            System.out.println("Server has started");
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    private Main(Path properties) throws Exception {
        if(!FILE.apply(properties))
            throw new IllegalArgumentException("Properties file doesn't exist");
        Properties srcProps;
        try(InputStream in = new FileInputStream((this.properties = properties).toFile())) {
            (srcProps = new Properties()).load(in);
        }
        int n = Integer.parseInt(srcProps.getProperty("capacity", "0"));
        PROCESSORS = new java.util.concurrent.ArrayBlockingQueue<>(n < MIN_LIMIT? n = MIN_LIMIT : n);
        for(int i = -1; ++i < n; )
            PROCESSORS.add(new Processor(this) {
                @Override
                protected void onFinish(Processor currentProcessor, String username, com.murez.entity.DataPackage<String> response) {
                    if(PROCESSORS.add(currentProcessor))
                        synchronized(PROCESSORS) {
                            String problem = response.getPackage().remove("problem");
                            if(Processor.TEXT.apply(problem)) {
                                System.err.println("There was a problem: " + problem);
                            }
                            System.out.printf("Username: %s (%s|%s)%n", username, response.getString(""), response.getPackage());
                        }
                }
            });
        final java.rmi.Remote STUB;
        try {
            STUB = UnicastRemoteObject.exportObject(new com.murez.net.RemoteAuthenticator(PROCESSORS), 0);
        } catch(Throwable e) {
            throw new IOException("Failed of initializing Stub", e);
        }
        try {
            LocateRegistry.getRegistry().rebind("Authentication", STUB);
        } catch(Throwable e) {
            throw new UnsupportedOperationException("Binding Stub to RMI Registry was unsuccessful", e);
        }
    }

    @Override
    public Connection getConnection() throws IOException {
        final String DATABASE, USERNAME, PASSWORD;
        Properties dbProps;
        synchronized(LOCK) {
            try(InputStream in = new FileInputStream(properties.toFile())) {
                (dbProps = new Properties()).load(in);
                DATABASE = dbProps.getProperty("database", "");
                USERNAME = dbProps.getProperty("username", "root");
                PASSWORD = dbProps.getProperty("password", "");
            } catch(Throwable e) {
                throw new IOException("Database properties can't be loaded", e);
            }
        }
        final Connection C;
        try {
            C = java.sql.DriverManager.getConnection("jdbc:mysql://localhost/" + DATABASE, USERNAME, PASSWORD);
        } catch(Throwable e) {
            throw new IOException("Attempting database connection was unsuccessful", e);
        }
        return C;
    }
}
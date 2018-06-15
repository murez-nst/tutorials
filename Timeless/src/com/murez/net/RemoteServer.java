package com.murez.net;

import com.murez.entity.DataPackage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
/**
 * @author Murez Nasution
 */
public class RemoteServer implements com.murez.remote.Authenticable {
    private final static Function<Path, Boolean> FILE = file -> Files.exists(file) && Files.isRegularFile(file);

    private final BlockingQueue<Processor> PROCESSORS;

    public RemoteServer(Path dbFile, int n) {
        PROCESSORS = new java.util.concurrent.ArrayBlockingQueue<>(n < 3? n = 3 : n);
        if(!FILE.apply(dbFile))
            throw new IllegalArgumentException("Properties file doesn't exist");
        //else this.dbFile = dbFile;
        for(int i = -1; ++i < n; )
            PROCESSORS.add(new Processor(dbFile, new RemoteServer.AuthListenable() {
                @Override
                public void onFinish(Processor currentProcessor, String username, DataPackage<String> response) {
                    PROCESSORS.add(currentProcessor);
                    System.out.printf("Username: %s (%s|%s)%n", username, response.getString(""), response.getPackage());
                }

                @Override
                public Path getFile() { return dbFile; }
            }));
    }

    @Override
    public void signin(String username, String password, DataPackage<String> dataPack) {
        Processor instance;
        try {
            instance = PROCESSORS.poll(5, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            instance = null;
        }
        if(instance == null)
            throw new UnsupportedOperationException("503 Service Unavailable");
        else
            try { instance.resume(username, password, dataPack); }
            catch(Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException("400 Bad Request", e);
            }
    }

    /*public void change(Path dbFile) {
        synchronized(LOCK) {
            if(FILE_VALIDATOR.apply(dbFile) && !dbFile.equals(this.dbFile))
                this.dbFile = dbFile;
        }
    }*/

    static Map.Entry<String, Boolean> userAuth(final Connection C, String username, String password) throws Exception {
        final String QUERY = "SELECT `Password` source, SHA1(?) target, `Name` FROM User WHERE `Email`=?";
        Map.Entry<String, Boolean> result;
        Boolean status;
        try(java.sql.PreparedStatement pS = C.prepareStatement(QUERY)) {
            pS.setString(1, password);
            pS.setString(2, username);
            try(java.sql.ResultSet rS = pS.executeQuery()) {
                if(rS.first()) {
                    status = rS.getString(1).equals(rS.getString(2));
                    result = new AbstractMap.SimpleImmutableEntry<>(rS.getString(3), status);
                }
                else
                    result = new AbstractMap.SimpleImmutableEntry<>(null, null);
            }
        }
        return result;
    }

    static Connection getConnection(Path src) throws IOException {
        final String DATABASE, USERNAME, PASSWORD;
        Properties dbProps;
        try(java.io.InputStream in = new java.io.FileInputStream(src.toFile())) {
            (dbProps = new Properties()).load(in);
            DATABASE = dbProps.getProperty("database", "");
            USERNAME = dbProps.getProperty("username", "root");
            PASSWORD = dbProps.getProperty("password", "");
        } catch(Throwable e) {
            throw new IOException("Database properties can't be loaded", e);
        }
        final Connection C;
        try {
            C = java.sql.DriverManager.getConnection("jdbc:mysql://localhost/" + DATABASE, USERNAME, PASSWORD);
        } catch(Throwable e) {
            throw new IOException("Attempting database connection was unsuccessful", e);
        }
        return C;
    }

    interface AuthListenable {
        void onFinish(Processor currentProcessor, String username, DataPackage<String> response);

        Path getFile();
    }
}
package com.murez.net;

import com.murez.entity.DataPackage;
import com.murez.util.Executor;
import java.math.BigInteger;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import static com.murez.net.RemoteServer.*;
/**
 * @author Murez Nasution
 */
public class Processor extends Executor<DataPackage<String>> {
    private final static Function<String, Boolean> TEXT = text -> text != null && text.length() > 0;
    private final static Random SELECTOR = new Random();

    private final RemoteServer.AuthListenable LISTENER;
    private DataPackage<String> dataPack;
    private String username, password;
    private Connection connector;

    Processor(java.nio.file.Path property, RemoteServer.AuthListenable listener) {
        try { connector = getConnection(property); }
        catch(Exception e) {
            System.err.printf(">> %s. %s%n", e.getMessage(), e.getCause().getMessage());
            connector = null;
        }
        if((LISTENER = listener) == null)
            throw new IllegalArgumentException();
    }

    void resume(String username, String password, DataPackage<String> dataPack) {
        Executor.Resumable notifier = acquires();
        if(notifier != null) {
            if(!TEXT.apply(username))
                throw new IllegalArgumentException("Username shouldn't be empty");
            else this.username = username;
            if(!TEXT.apply(password))
                throw new IllegalArgumentException("Password shouldn't be empty");
            else this.password = password;
            String[] keys = { "name" };
            if(dataPack == null
                || dataPack.getNumber(null) == null
                || dataPack.getString(null) == null
                || !dataPack.getPackage().keySet().containsAll(Arrays.asList(keys)))
                throw new UnsupportedOperationException("Properties of client's remote callback should be defined correctly");
            else this.dataPack = dataPack;
            notifier.resume();
        }
    }

    @Override
    public DataPackage<String> onActive() {
        Map.Entry<String, Boolean> result;
        try {
            refresh();
            result = userAuth(connector, username, password);
        } catch(Exception e) {
            e.printStackTrace();
            return DataPackage.create(500, "500 Internal Server Error");
        }
        DataPackage<String> response = DataPackage.create(200, "200 OK");
        Boolean status = result.getValue();
        String message = null;
        if(status == null) {
            message = "Email doesn't exist";
        } else
        if(status) {
            response.getPackage().put("session", BigInteger.probablePrime(128, SELECTOR).toString());
            response.getPackage().put("name", result.getKey());
        } else
            message = "Password was wrong";
        response.getPackage().put("message", message);
        return response;
    }

    @Override
    public void onFinish(DataPackage<String> response) {
        try { report(response, dataPack); }
        catch(Throwable e) {
            e.printStackTrace();
        }
        LISTENER.onFinish(this, username, response);
    }

    @Override
    public void onClose() {
        try {
            if(!connector.isClosed())
                connector.close();
        } catch(Exception e) { connector = null; }
    }

    private void refresh() throws Exception {
        try {
            if(!connector.isClosed()) return;
        } catch(Exception e) { /*ignored*/ }
        for(int i = 1;; ++i) {
            try { connector = getConnection(LISTENER.getFile()); }
            catch(Exception e) {
                if(i < 3) {
                    Thread.sleep(5500);
                    continue;
                }
                throw e;
            }
            break;
        }
    }

    private static void report(DataPackage response, DataPackage<String> dataPack) throws Throwable {
        java.rmi.Remote remote = java.rmi.registry.LocateRegistry.getRegistry(
                dataPack.getString(null),
                dataPack.getNumber(0).intValue()
            ).lookup(dataPack.getPackage().get("name"));
        remote.getClass()
            .getMethod("onFinish", DataPackage.class)
            .invoke(remote, response);
    }
}
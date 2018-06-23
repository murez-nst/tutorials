package com.murez.util;

import com.murez.entity.DataPackage;
import com.murez.sql.SQLMaintainable;
import java.math.BigInteger;
import java.sql.Connection;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
/**
 * @author Murez Nasution
 */
public abstract class Processor extends Executor<DataPackage<String>> {
    public final static Function<String, Boolean> TEXT = text -> text != null && text.length() > 0;
    private final static Random SELECTOR = new Random();

    private final SQLMaintainable PROVIDER;
    private DataPackage<String> dataPack;
    private String username, password;
    private Connection connector;

    private static Map.Entry<String, Boolean> userAuth(final Connection C, String username, String password) throws Exception {
        final String QUERY = "SELECT `Password` source, SHA1(?) target, `Name` FROM User WHERE `Email`=?";
        Map.Entry<String, Boolean> result;
        try(java.sql.PreparedStatement pS = C.prepareStatement(QUERY)) {
            pS.setString(1, password);
            pS.setString(2, username);
            try(java.sql.ResultSet rS = pS.executeQuery()) {
                if(rS.first())
                    result = new AbstractMap.SimpleImmutableEntry<>(rS.getString(3), rS.getString(1).equals(rS.getString(2)));
                else
                    result = new AbstractMap.SimpleImmutableEntry<>(null, null);
            }
        }
        return result;
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

    protected Processor(SQLMaintainable provider) {
        try { connector = provider.getConnection(); }
        catch(NullPointerException e) { throw e; }
        catch(Exception e) {
            System.err.printf(">> %s. %s%n", e.getMessage(), e.getCause().getMessage());
            connector = null;
        }
        PROVIDER = provider;
    }

    public void resume(String username, String password, DataPackage<String> dataPack) {
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
                throw new IllegalArgumentException("Properties of client's remote callback should be defined correctly");
            else this.dataPack = dataPack;
            notifier.resume();
        } else
        throw new UnsupportedOperationException("This processor was unavailable");
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
            response.getPackage().put("problem", e.getMessage());
        }
        onFinish(this, username, response);
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
            try { connector = PROVIDER.getConnection(); }
            catch(Exception e) {
                if(i < 3) {
                    Thread.sleep(1500);
                    continue;
                }
                throw e;
            }
            break;
        }
    }

    protected abstract void onFinish(Processor currentProcessor, String username, DataPackage<String> response);
}
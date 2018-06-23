package com.murez.net;

import com.murez.util.Processor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
/**
 * @author Murez Nasution
 */
public class RemoteAuthenticator implements com.murez.remote.Authenticable {
    private final BlockingQueue<Processor> PROCESSORS;

    public RemoteAuthenticator(BlockingQueue<Processor> processors) {
        PROCESSORS = processors;
    }

    @Override
    public void signin(String username, String password, com.murez.entity.DataPackage<String> dataPack) {
        Processor instance;
        try {
            instance = PROCESSORS.poll(3, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            instance = null;
        }
        if(instance == null)
            throw new UnsupportedOperationException("503 Service Unavailable");
        else
            try { instance.resume(username, password, dataPack); }
            catch(IllegalArgumentException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("400 Bad Request", e);
            }
            catch(Exception e) {
                throw new UnsupportedOperationException("500 Internal Server Error", e);
            }
    }
}
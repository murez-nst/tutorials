package com.julisa.remote;
/**
 * @author Murez Nasution
 */
public interface EventListenable extends java.rmi.Remote {
    void onFinish(com.murez.entity.DataPackage<String> response) throws java.rmi.RemoteException;
}
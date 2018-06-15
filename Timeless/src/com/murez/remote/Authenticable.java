package com.murez.remote;

import com.murez.entity.DataPackage;
/**
 * @author Murez Nasution
 */
public interface Authenticable extends java.rmi.Remote {
    void signin(String username, String password, DataPackage<String> dataPack) throws java.rmi.RemoteException;
}
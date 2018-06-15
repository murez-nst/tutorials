package com.murez.entity;

import java.util.Map;
/**
 * @author Murez Nasution
 */
public class DataPackage<T> implements java.io.Serializable {
    private final Number N;
    private final String S;
    private final Map<String, T> PACKAGE;

    public DataPackage(Number n, String s, Map<String, T> instance) {
        N = n;
        S = s;
        if((PACKAGE = instance) == null)
            throw new IllegalArgumentException();
    }

    public static DataPackage<String> create(Number n, String s) {
        return new DataPackage<>(n, s, new java.util.HashMap<>());
    }

    public final Number getNumber(Number defaultValue) { return N == null? defaultValue : N; }

    public final String getString(String defaultValue) { return S != null && S.length() > 0? S: defaultValue; }

    public Map<String, T> getPackage() { return PACKAGE; }
}
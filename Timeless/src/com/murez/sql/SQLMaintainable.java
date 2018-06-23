package com.murez.sql;
/**
 * @author Murez Nasution
 */
public interface SQLMaintainable {
    java.sql.Connection getConnection() throws java.io.IOException;
}
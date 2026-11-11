package com.netfieldpro.data.db;

@FunctionalInterface
public interface WriteBatch {
    void run(NetFieldDatabase db) throws Exception;
}


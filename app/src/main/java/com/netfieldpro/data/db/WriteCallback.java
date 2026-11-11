package com.netfieldpro.data.db;

public interface WriteCallback {
    void onSuccess();
    void onError(Throwable t);
}


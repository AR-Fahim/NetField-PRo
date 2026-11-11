package com.netfieldpro.core.integrity;

public interface Signer {
    byte[] signDer(byte[] msg);
    byte[] publicKeyCompressed();
}

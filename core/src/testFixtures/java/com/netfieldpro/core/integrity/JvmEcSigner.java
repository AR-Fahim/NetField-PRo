package com.netfieldpro.core.integrity;

import com.netfieldpro.core.util.EcdsaCodec;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;

public final class JvmEcSigner implements Signer {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final byte[] compressedPublicKey;

    public JvmEcSigner() {
        this(generateKeyPair());
    }

    public JvmEcSigner(KeyPair keyPair) {
        this(keyPair.getPrivate(), keyPair.getPublic());
    }

    public JvmEcSigner(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        if (publicKey instanceof ECPublicKey ecPub) {
            ECPoint w = ecPub.getW();
            byte[] x = w.getAffineX().toByteArray();
            byte[] y = w.getAffineY().toByteArray();
            this.compressedPublicKey = EcdsaCodec.compressPoint(x, y);
        } else {
            throw new IllegalArgumentException("Public key must be ECPublicKey");
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate EC key pair", e);
        }
    }

    @Override
    public byte[] signDer(byte[] msg) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(msg);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign msg with ECDSA", e);
        }
    }

    @Override
    public byte[] publicKeyCompressed() {
        return compressedPublicKey.clone();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}

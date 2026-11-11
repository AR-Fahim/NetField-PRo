package com.netfieldpro.integrity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.netfieldpro.core.integrity.Signer;
import com.netfieldpro.core.util.EcdsaCodec;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;

public class AndroidSigner implements Signer {

    public static final String DEFAULT_ALIAS = "nf_signing_v1";
    private final String alias;

    public AndroidSigner(Context context) {
        this(context, DEFAULT_ALIAS);
    }

    public AndroidSigner(Context context, String alias) {
        this.alias = alias;
        ensureKey(context, alias);
    }

    private static void ensureKey(Context context, String alias) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (ks.containsAlias(alias)) {
                return;
            }

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
            );
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN
            )
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256);

            boolean hasStrongBox = context != null
                    && context.getPackageManager() != null
                    && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE);

            if (hasStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    builder.setIsStrongBoxBacked(true);
                    kpg.initialize(builder.build());
                    kpg.generateKeyPair();
                    return;
                } catch (Exception e) {
                    builder.setIsStrongBoxBacked(false);
                }
            }

            kpg.initialize(builder.build());
            kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure AndroidKeyStore key for alias: " + alias, e);
        }
    }

    @Override
    public byte[] signDer(byte[] msg) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            PrivateKey priv = (PrivateKey) ks.getKey(alias, null);
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(priv);
            sig.update(msg);
            return sig.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign msg with AndroidKeyStore key", e);
        }
    }

    @Override
    public byte[] publicKeyCompressed() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            Certificate cert = ks.getCertificate(alias);
            if (cert == null) {
                throw new IllegalStateException("Certificate not found for alias: " + alias);
            }
            PublicKey pub = cert.getPublicKey();
            if (pub instanceof ECPublicKey ecPub) {
                ECPoint w = ecPub.getW();
                byte[] x = w.getAffineX().toByteArray();
                byte[] y = w.getAffineY().toByteArray();
                return EcdsaCodec.compressPoint(x, y);
            }
            throw new IllegalStateException("Public key is not an ECPublicKey");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get compressed public key", e);
        }
    }
}

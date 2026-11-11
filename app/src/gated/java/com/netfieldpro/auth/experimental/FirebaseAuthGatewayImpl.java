package com.netfieldpro.auth.experimental;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

import com.netfieldpro.BuildConfig;
import com.netfieldpro.auth.AuthCallback;
import com.netfieldpro.auth.AuthError;
import com.netfieldpro.auth.AuthSessionGateway;
import com.netfieldpro.core.auth.AuthErrorMapper;
import com.netfieldpro.core.auth.AuthOutcome;

public class FirebaseAuthGatewayImpl implements AuthSessionGateway {

    private static final String TAG = "NF_FACT";
    private final Context context;

    public FirebaseAuthGatewayImpl(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public static synchronized void ensureInit(Context ctx) {
        if (ctx == null) {
            return;
        }
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(ctx, new FirebaseOptions.Builder()
                    .setApplicationId(BuildConfig.FB_APP_ID)
                    .setApiKey(BuildConfig.FB_API_KEY)
                    .setProjectId(BuildConfig.FB_PROJECT_ID)
                    .build());
        }
    }

    private FirebaseAuth getAuth() {
        if (context != null) {
            ensureInit(context);
        }
        return FirebaseAuth.getInstance();
    }

    @Override
    public boolean hasActiveSession() {
        try {
            FirebaseAuth auth = getAuth();
            return auth.getCurrentUser() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean supportsAccounts() {
        return true;
    }

    @Override
    public String currentEmail() {
        try {
            FirebaseUser user = getAuth().getCurrentUser();
            return user != null ? user.getEmail() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getCurrentUserEmail() {
        return currentEmail();
    }

    @Override
    public void signIn(String email, String password, Callback cb) {
        try {
            FirebaseAuth auth = getAuth();
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        AuthOutcome outcome;
                        if (task.isSuccessful()) {
                            outcome = AuthOutcome.OK;
                        } else {
                            outcome = mapException(task.getException());
                        }
                        Log.i(TAG, "auth op=signIn outcome=" + outcome.name());
                        dispatchCallback(cb, outcome);
                    });
        } catch (Exception e) {
            AuthOutcome outcome = mapException(e);
            Log.i(TAG, "auth op=signIn outcome=" + outcome.name());
            dispatchCallback(cb, outcome);
        }
    }

    @Override
    public void signUp(String email, String password, Callback cb) {
        try {
            FirebaseAuth auth = getAuth();
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        AuthOutcome outcome;
                        if (task.isSuccessful()) {
                            outcome = AuthOutcome.OK;
                        } else {
                            outcome = mapException(task.getException());
                        }
                        Log.i(TAG, "auth op=signUp outcome=" + outcome.name());
                        dispatchCallback(cb, outcome);
                    });
        } catch (Exception e) {
            AuthOutcome outcome = mapException(e);
            Log.i(TAG, "auth op=signUp outcome=" + outcome.name());
            dispatchCallback(cb, outcome);
        }
    }

    @Override
    public void sendPasswordReset(String email, Callback cb) {
        try {
            FirebaseAuth auth = getAuth();
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        AuthOutcome outcome;
                        if (task.isSuccessful()) {
                            outcome = AuthOutcome.OK;
                        } else {
                            outcome = mapException(task.getException());
                        }
                        Log.i(TAG, "auth op=reset outcome=" + outcome.name());
                        dispatchCallback(cb, outcome);
                    });
        } catch (Exception e) {
            AuthOutcome outcome = mapException(e);
            Log.i(TAG, "auth op=reset outcome=" + outcome.name());
            dispatchCallback(cb, outcome);
        }
    }

    @Override
    public void signOut() {
        try {
            getAuth().signOut();
            Log.i(TAG, "auth op=signOut outcome=OK");
        } catch (Exception e) {
            Log.i(TAG, "auth op=signOut outcome=UNKNOWN");
        }
    }

    @Override
    public void signIn(String email, String pw, AuthCallback cb) {
        signIn(email, pw, outcome -> {
            if (cb != null) {
                if (outcome == AuthOutcome.OK) {
                    cb.onSuccess();
                } else {
                    cb.onError(AuthError.UNKNOWN);
                }
            }
        });
    }

    @Override
    public void register(String email, String pw, AuthCallback cb) {
        signUp(email, pw, outcome -> {
            if (cb != null) {
                if (outcome == AuthOutcome.OK) {
                    cb.onSuccess();
                } else {
                    cb.onError(AuthError.UNKNOWN);
                }
            }
        });
    }

    @Override
    public void sendPasswordReset(String email, AuthCallback cb) {
        sendPasswordReset(email, outcome -> {
            if (cb != null) {
                if (outcome == AuthOutcome.OK) {
                    cb.onSuccess();
                } else {
                    cb.onError(AuthError.UNKNOWN);
                }
            }
        });
    }

    private static AuthOutcome mapException(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return AuthOutcome.NO_NETWORK;
        } else if (e instanceof FirebaseTooManyRequestsException) {
            return AuthOutcome.TOO_MANY_REQUESTS;
        } else if (e instanceof FirebaseAuthException) {
            return AuthErrorMapper.map(((FirebaseAuthException) e).getErrorCode());
        } else {
            return AuthOutcome.UNKNOWN;
        }
    }

    private static void dispatchCallback(Callback cb, AuthOutcome outcome) {
        if (cb == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            cb.onResult(outcome);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> cb.onResult(outcome));
        }
    }
}

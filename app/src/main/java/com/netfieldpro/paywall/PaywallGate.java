package com.netfieldpro.paywall;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.fragment.NavHostFragment;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.DecisionKind;
import com.netfieldpro.core.entitlement.EntitlementDecision;
import com.netfieldpro.core.entitlement.EntitlementPolicy;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.ui.NavArgs;

public final class PaywallGate {

    private static final String TAG = "PaywallGate";

    public enum GateOutcome {
        RUN,
        SHOW_PAYWALL,
        IGNORE
    }

    private PaywallGate() {}

    public static GateOutcome evaluate(@NonNull Tier tier, @NonNull Feature feature) {
        EntitlementDecision decision = EntitlementPolicy.decide(tier, feature);
        switch (decision.kind()) {
            case ALLOWED:
                return GateOutcome.RUN;
            case LOCKED:
                return GateOutcome.SHOW_PAYWALL;
            case NOT_APPLICABLE:
            default:
                return GateOutcome.IGNORE;
        }
    }

    public static boolean require(@NonNull Fragment host, @NonNull Feature f, @NonNull Runnable onAllowed) {
        Tier currentTier = NetFieldApp.entitlements().current();
        EntitlementDecision decision = EntitlementPolicy.decide(currentTier, f);

        if (decision.kind() == DecisionKind.ALLOWED) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onAllowed.run();
            } else {
                new Handler(Looper.getMainLooper()).post(onAllowed);
            }
            return true;
        }

        if (decision.kind() == DecisionKind.NOT_APPLICABLE) {
            Log.w(TAG, "Feature not applicable for tier: " + f);
            return false;
        }

        // Decision is LOCKED
        FragmentManager fm = host.getParentFragmentManager();
        LifecycleOwner owner;
        try {
            owner = host.getViewLifecycleOwner();
        } catch (IllegalStateException e) {
            owner = host;
        }

        fm.setFragmentResultListener(NavArgs.RESULT_KEY_PAYWALL, owner, (requestKey, result) -> {
            fm.clearFragmentResultListener(NavArgs.RESULT_KEY_PAYWALL);
            String res = result.getString(NavArgs.RESULT_VALUE);
            if (NavArgs.RESULT_PURCHASED.equals(res)) {
                Tier newTier = NetFieldApp.entitlements().current();
                EntitlementDecision newDecision = EntitlementPolicy.decide(newTier, f);
                if (newDecision.kind() == DecisionKind.ALLOWED) {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        onAllowed.run();
                    } else {
                        new Handler(Looper.getMainLooper()).post(onAllowed);
                    }
                }
            }
        });

        Bundle args = new Bundle();
        args.putString(NavArgs.ARG_FEATURE, f.name());
        NavHostFragment.findNavController(host).navigate(R.id.action_global_paywall, args);
        return false;
    }
}


package com.netfieldpro.entitlement;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.netfieldpro.core.entitlement.Tier;

public class EntitlementRepository {

    private final PrefsTierSource tierSource;
    private final MutableLiveData<Tier> liveTier;

    public EntitlementRepository(PrefsTierSource tierSource) {
        this.tierSource = tierSource;
        this.liveTier = new MutableLiveData<>(tierSource.current());
    }

    public Tier current() {
        return tierSource.current();
    }

    public LiveData<Tier> tier() {
        return liveTier;
    }

    public void setDebugTier(Tier tier) {
        tierSource.setDebugTier(tier);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            liveTier.setValue(tierSource.current());
        } else {
            new Handler(Looper.getMainLooper()).post(() -> liveTier.setValue(tierSource.current()));
        }
    }
}

package com.netfieldpro.latency;

import android.net.Network;

import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeSequence;

public interface LatencyRunner {

    interface Listener {
        void onRunning(ProbeCandidate candidate);
        void onCompleted(ProbeCandidate candidate, ProbeSequence.SequenceResult result);
        void onBlocked();
        void onNetworkLost();
        void onCancelled();
    }

    void start(Network network, String gateway, String dns, Listener listener);
    void cancel();
    void onNetworkLost();
    void shutdown();
    boolean isRunning();
}

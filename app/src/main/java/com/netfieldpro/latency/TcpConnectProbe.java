package com.netfieldpro.latency;

import android.net.Network;

import com.netfieldpro.core.latency.IcmpProbe;
import com.netfieldpro.core.latency.ProbeResult;
import com.netfieldpro.core.latency.TcpOutcome;
import com.netfieldpro.core.time.Clock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.BooleanSupplier;

public class TcpConnectProbe implements IcmpProbe {

    private final Network network;
    private final String target;
    private final int port;
    private final Clock clock;
    private final BooleanSupplier isAborted;

    public TcpConnectProbe(Network network, String target, int port, Clock clock, BooleanSupplier isAborted) {
        this.network = network;
        this.target = target;
        this.port = port;
        this.clock = clock;
        this.isAborted = isAborted;
    }

    @Override
    public ProbeResult probe(int seq, long timeoutNs) {
        if (isAborted != null && isAborted.getAsBoolean()) {
            return ProbeResult.aborted();
        }

        Socket socket = null;
        try {
            if (network != null) {
                socket = network.getSocketFactory().createSocket();
            } else {
                socket = new Socket();
            }

            int timeoutMs = (int) (timeoutNs / 1_000_000L);
            InetAddress addr = InetAddress.getByName(target);
            InetSocketAddress endpoint = new InetSocketAddress(addr, port);

            long startNs = clock.monoNs();
            try {
                socket.connect(endpoint, timeoutMs);
                long rttNs = clock.monoNs() - startNs;
                return ProbeResult.ok(rttNs / 1000L);
            } catch (Throwable t) {
                long rttNs = clock.monoNs() - startNs;
                TcpOutcome outcome = TcpOutcome.classify(t);
                if (outcome == TcpOutcome.REFUSED) {
                    return ProbeResult.ok(rttNs / 1000L);
                }
                if (isAborted != null && isAborted.getAsBoolean()) {
                    return ProbeResult.aborted();
                }
                return ProbeResult.lost();
            }
        } catch (Throwable t) {
            if (isAborted != null && isAborted.getAsBoolean()) {
                return ProbeResult.aborted();
            }
            return ProbeResult.lost();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}

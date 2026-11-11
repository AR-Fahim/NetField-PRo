package com.netfieldpro.core.latency;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

public enum TcpOutcome {
    OK,
    REFUSED,
    LOST;

    public boolean isCountedAsOk() {
        return this == OK || this == REFUSED;
    }

    public static TcpOutcome classify(Throwable t) {
        if (t == null) {
            return OK;
        }
        if (t instanceof SocketTimeoutException) {
            return LOST;
        }
        if (t instanceof ConnectException) {
            String msg = t.getMessage();
            if (msg != null && (msg.contains("ECONNREFUSED") || msg.contains("Connection refused"))) {
                return REFUSED;
            }
            return LOST;
        }
        return LOST;
    }
}

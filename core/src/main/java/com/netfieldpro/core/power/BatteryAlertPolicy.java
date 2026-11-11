package com.netfieldpro.core.power;

import com.netfieldpro.core.power.BatteryAdvisor.Level;

public final class BatteryAlertPolicy {

    private BatteryAlertPolicy() {
    }

    public static NotifyAction decide(Level prev, Level next) {
        if (prev == next) {
            return NotifyAction.NONE;
        }
        if ((prev == Level.NONE && next == Level.WARN)
                || (prev == Level.NONE && next == Level.CRITICAL)
                || (prev == Level.WARN && next == Level.CRITICAL)) {
            return NotifyAction.POST_ALERT;
        }
        if (prev == Level.CRITICAL && next == Level.WARN) {
            return NotifyAction.UPDATE_QUIET;
        }
        if ((prev == Level.WARN && next == Level.NONE)
                || (prev == Level.CRITICAL && next == Level.NONE)) {
            return NotifyAction.CANCEL;
        }
        return NotifyAction.NONE;
    }
}


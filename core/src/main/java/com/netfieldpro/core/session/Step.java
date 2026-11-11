package com.netfieldpro.core.session;

import java.util.List;

public record Step(Sm1State state, EndReason reason, List<Effect> effects) {

    public String effectsText() {
        if (effects == null || effects.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < effects.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(effects.get(i).toString());
        }
        return sb.toString();
    }
}

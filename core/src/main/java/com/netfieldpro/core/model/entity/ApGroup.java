package com.netfieldpro.core.model.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.ApGroupOrigin;

public record ApGroup(
        String id,
        String sessionId,
        String label,
        ApGroupOrigin origin,
        List<String> memberBssids
) {
    public ApGroup {
        ModelValidator.checkId("ApGroup", "id", id);
        ModelValidator.checkId("ApGroup", "sessionId", sessionId);
        ModelValidator.checkRequired("ApGroup", "label", label);
        ModelValidator.checkString("ApGroup", "label", label, 1024);
        ModelValidator.checkRequired("ApGroup", "origin", origin);
        memberBssids = ModelValidator.checkList("ApGroup", "memberBssids", memberBssids);
        List<String> sorted = new ArrayList<>(memberBssids);
        Collections.sort(sorted);
        memberBssids = List.copyOf(sorted);
    }
}


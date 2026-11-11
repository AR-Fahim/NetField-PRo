package com.netfieldpro.entitlement;

import com.netfieldpro.core.entitlement.Tier;

public interface TierSource {
    Tier current();
}

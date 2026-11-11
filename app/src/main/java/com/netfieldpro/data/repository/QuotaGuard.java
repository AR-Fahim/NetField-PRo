package com.netfieldpro.data.repository;

import java.util.Objects;
import java.util.function.Supplier;

import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.QuotaPolicy;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.entitlement.EntitlementRepository;

public class QuotaGuard {

    private final NetFieldDao dao;
    private final Supplier<Tier> tierSupplier;

    public QuotaGuard(NetFieldDao dao, EntitlementRepository entitlementRepository) {
        this(dao, entitlementRepository::current);
    }

    public QuotaGuard(NetFieldDao dao, Supplier<Tier> tierSupplier) {
        this.dao = Objects.requireNonNull(dao, "dao cannot be null");
        this.tierSupplier = Objects.requireNonNull(tierSupplier, "tierSupplier cannot be null");
    }

    public static QuotaGuard create(android.content.Context context, EntitlementRepository entitlementRepository) {
        return new QuotaGuard(
                com.netfieldpro.data.db.NetFieldDatabase.get(context).netFieldDao(),
                entitlementRepository
        );
    }

    public QuotaDecision check(Resource resource) {
        Objects.requireNonNull(resource, "resource cannot be null");
        int count = switch (resource) {
            case PROJECT -> dao.countProjects();
            case FLOOR -> dao.countFloors();
            case SESSION -> dao.countSurveySessions();
        };
        Tier tier = tierSupplier.get();
        return QuotaPolicy.canCreate(tier, resource, count);
    }
}

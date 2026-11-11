package com.netfieldpro.data.seed;

import java.util.Map;
import java.util.TreeMap;

import com.netfieldpro.core.model.entity.RequirementProfile;
import com.netfieldpro.core.model.enums.BandPolicy;
import com.netfieldpro.core.model.enums.ProfileKind;
import com.netfieldpro.core.verdict.CritId;
import com.netfieldpro.core.verdict.PresetProfiles;
import com.netfieldpro.core.verdict.ProfileSpec;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.mapper.RequirementProfileMapper;

public final class ProfileSeeder {

    private ProfileSeeder() {
    }

    public static void seed(NetFieldDao dao) {
        if (dao == null) {
            return;
        }
        for (ProfileSpec spec : PresetProfiles.all()) {
            String id = PresetProfiles.idFor(spec.key());
            if (dao.getRequirementProfile(id) == null) {
                Map<String, Boolean> criterionEnabled = new TreeMap<>();
                for (CritId cid : CritId.values()) {
                    criterionEnabled.put(cid.name(), true);
                }
                RequirementProfile rp = new RequirementProfile(
                        id,
                        ProfileKind.PRESET,
                        PresetProfiles.thresholdsJson(spec),
                        BandPolicy.BEST_ANY,
                        spec.maxInfluenceM(),
                        criterionEnabled
                );
                dao.insertRequirementProfile(RequirementProfileMapper.toRow(rp));
            }
        }
    }
}


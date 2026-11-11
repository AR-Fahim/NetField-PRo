package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;

public record Project(
        String id,
        String name,
        String client,
        String site,
        long createdAtMs
) {
    public Project {
        ModelValidator.checkId("Project", "id", id);
        ModelValidator.checkRequired("Project", "name", name);
        ModelValidator.checkString("Project", "name", name, 1024);
        ModelValidator.checkString("Project", "client", client, 1024);
        ModelValidator.checkString("Project", "site", site, 1024);
        ModelValidator.checkNonNegative("Project", "createdAtMs", createdAtMs);
    }
}


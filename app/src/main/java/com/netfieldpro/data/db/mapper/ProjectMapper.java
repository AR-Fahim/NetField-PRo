package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.data.db.entity.ProjectRow;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectRow toRow(Project p) {
        ProjectRow row = new ProjectRow();
        row.id = p.id();
        row.name = p.name();
        row.client = p.client();
        row.site = p.site();
        row.createdAtMs = p.createdAtMs();
        return row;
    }

    public static Project toDomain(ProjectRow row) {
        return new Project(row.id, row.name, row.client, row.site, row.createdAtMs);
    }
}


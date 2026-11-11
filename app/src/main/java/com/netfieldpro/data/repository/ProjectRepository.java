package com.netfieldpro.data.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.project.NameRules;
import com.netfieldpro.core.project.ProjectSort;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.mapper.ProjectMapper;
import com.netfieldpro.time.AndroidClock;

public class ProjectRepository {

    private final NetFieldDatabase db;
    private final NetFieldDao dao;
    private final BatchedWriter writer;
    private final PlanFileStore planFileStore;
    private final QuotaGuard quotaGuard;
    private final Clock clock;

    public ProjectRepository(
            NetFieldDatabase db,
            BatchedWriter writer,
            PlanFileStore planFileStore,
            QuotaGuard quotaGuard
    ) {
        this(db, writer, planFileStore, quotaGuard, new AndroidClock());
    }

    public ProjectRepository(
            NetFieldDatabase db,
            BatchedWriter writer,
            PlanFileStore planFileStore,
            QuotaGuard quotaGuard,
            Clock clock
    ) {
        this.db = Objects.requireNonNull(db, "db cannot be null");
        this.dao = db.netFieldDao();
        this.writer = Objects.requireNonNull(writer, "writer cannot be null");
        this.planFileStore = Objects.requireNonNull(planFileStore, "planFileStore cannot be null");
        this.quotaGuard = Objects.requireNonNull(quotaGuard, "quotaGuard cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public static ProjectRepository create(
            android.content.Context context,
            com.netfieldpro.entitlement.EntitlementRepository entitlements
    ) {
        NetFieldDatabase db = NetFieldDatabase.get(context);
        return new ProjectRepository(
                db,
                new BatchedWriter(db),
                new PlanFileStore(context),
                new QuotaGuard(db.netFieldDao(), entitlements),
                new AndroidClock()
        );
    }

    public List<Project> getProjects() {
        List<ProjectRow> rows = dao.getAllProjects();
        List<Project> list = new ArrayList<>();
        if (rows != null) {
            for (ProjectRow r : rows) {
                list.add(ProjectMapper.toDomain(r));
            }
        }
        return ProjectSort.sort(list);
    }

    public Project getProject(String id) {
        ProjectRow row = dao.getProject(id);
        return row != null ? ProjectMapper.toDomain(row) : null;
    }

    public Project createProject(String name, String client, String site) {
        NameRules.Status status = NameRules.validate(name);
        if (status != NameRules.Status.OK) {
            throw new IllegalArgumentException("Invalid project name: " + status);
        }
        QuotaDecision decision = quotaGuard.check(Resource.PROJECT);
        if (!decision.allowed()) {
            throw new IllegalStateException("PROJECT quota exceeded");
        }

        String normName = NameRules.normalize(name);
        String normSite = NameRules.normalizeSite(site);
        String id = IdGenerator.generate();
        long now = clock.wallMs();

        Project project = new Project(id, normName, client, normSite, now);
        ProjectRow row = ProjectMapper.toRow(project);

        writer.submit(database -> database.netFieldDao().insertProject(row), null);
        writer.flush();
        return project;
    }

    public void renameProject(String id, String newName) {
        NameRules.Status status = NameRules.validate(newName);
        if (status != NameRules.Status.OK) {
            throw new IllegalArgumentException("Invalid project name: " + status);
        }
        String normName = NameRules.normalize(newName);
        writer.submit(database -> database.netFieldDao().updateProjectName(id, normName), null);
        writer.flush();
    }

    public void delete(String id) {
        deleteProject(id);
    }

    public void deleteProject(String id) {
        List<FloorRow> floors = dao.getFloorsForProject(id);
        if (floors != null) {
            for (FloorRow f : floors) {
                planFileStore.deletePlanFile(f.id);
            }
        }
        writer.submit(database -> database.netFieldDao().deleteProject(id), null);
        writer.flush();
    }
}

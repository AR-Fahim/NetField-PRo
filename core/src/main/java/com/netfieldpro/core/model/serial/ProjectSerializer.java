package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.Project;

public final class ProjectSerializer {

    private ProjectSerializer() {
    }

    public static JsonObject toJson(Project p) {
        JsonObject obj = new JsonObject();
        obj.put("createdAtMs", new JsonInt(p.createdAtMs()));
        obj.put("id", new JsonString(p.id()));
        obj.put("name", new JsonString(p.name()));
        if (p.client() != null) {
            obj.put("client", new JsonString(p.client()));
        }
        if (p.site() != null) {
            obj.put("site", new JsonString(p.site()));
        }
        return obj;
    }

    public static Project fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String name = ((JsonString) obj.get("name")).value();
        String client = obj.get("client") instanceof JsonString s ? s.value() : null;
        String site = obj.get("site") instanceof JsonString s ? s.value() : null;
        long createdAtMs = IntFields.parseLong(obj.get("createdAtMs"), "createdAtMs");
        return new Project(id, name, client, site, createdAtMs);
    }
}

package com.netfieldpro.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.netfieldpro.core.session.Sm1State;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SessionRuntimeStore {

    public static final String PREFS_NAME = "nf_session_runtime";
    public static final int MAX_GAPS = 500;

    public record Gap(String kind, long tNs) {}

    public record Entry(Sm1State state, long lastActivityNs, List<Gap> gaps) {}

    private final SharedPreferences prefs;

    public SessionRuntimeStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static SessionRuntimeStore create(Context context) {
        return new SessionRuntimeStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        );
    }

    public synchronized Sm1State getState(String sessionId) {
        if (sessionId == null) {
            return Sm1State.ENDED;
        }
        Entry entry = getEntry(sessionId);
        return entry != null ? entry.state() : Sm1State.ENDED;
    }

    public synchronized Entry getEntry(String sessionId) {
        if (sessionId == null) {
            return new Entry(Sm1State.ENDED, 0L, Collections.emptyList());
        }
        String json = prefs.getString(sessionId, null);
        if (json == null || json.isEmpty()) {
            return new Entry(Sm1State.ENDED, 0L, Collections.emptyList());
        }
        try {
            JSONObject obj = new JSONObject(json);
            String stateStr = obj.optString("s", Sm1State.ENDED.name());
            Sm1State state;
            try {
                state = Sm1State.valueOf(stateStr);
            } catch (Exception e) {
                state = Sm1State.ENDED;
            }
            long activity = obj.optLong("a", 0L);
            List<Gap> gaps = new ArrayList<>();
            JSONArray gArr = obj.optJSONArray("g");
            if (gArr != null) {
                for (int i = 0; i < gArr.length(); i++) {
                    JSONArray pair = gArr.optJSONArray(i);
                    if (pair != null && pair.length() >= 2) {
                        String kind = pair.optString(0);
                        long t = pair.optLong(1);
                        gaps.add(new Gap(kind, t));
                    }
                }
            }
            return new Entry(state, activity, gaps);
        } catch (Exception e) {
            return new Entry(Sm1State.ENDED, 0L, Collections.emptyList());
        }
    }

    public synchronized void putState(String sessionId, Sm1State state) {
        if (sessionId == null) return;
        Entry entry = getEntry(sessionId);
        putEntry(sessionId, state, entry.lastActivityNs(), entry.gaps());
    }

    public synchronized void putEntry(String sessionId, Sm1State state, long lastActivityNs, List<Gap> gaps) {
        if (sessionId == null) return;
        Sm1State targetState = state != null ? state : Sm1State.ENDED;
        List<Gap> targetGaps = gaps != null ? gaps : Collections.emptyList();
        if (targetGaps.size() > MAX_GAPS) {
            targetGaps = targetGaps.subList(targetGaps.size() - MAX_GAPS, targetGaps.size());
        }

        try {
            JSONObject obj = new JSONObject();
            obj.put("s", targetState.name());
            obj.put("a", lastActivityNs);
            JSONArray gArr = new JSONArray();
            for (Gap gap : targetGaps) {
                JSONArray pair = new JSONArray();
                pair.put(gap.kind());
                pair.put(gap.tNs());
                gArr.put(pair);
            }
            obj.put("g", gArr);
            prefs.edit().putString(sessionId, obj.toString()).commit();
        } catch (Exception ignored) {
        }
    }

    public synchronized void appendGap(String sessionId, String gapKind, long tNs) {
        if (sessionId == null) return;
        Entry entry = getEntry(sessionId);
        List<Gap> gaps = new ArrayList<>(entry.gaps());
        gaps.add(new Gap(gapKind, tNs));
        if (gaps.size() > MAX_GAPS) {
            gaps = gaps.subList(gaps.size() - MAX_GAPS, gaps.size());
        }
        putEntry(sessionId, entry.state(), entry.lastActivityNs(), gaps);
    }

    public synchronized void updateActivity(String sessionId, long lastActivityNs) {
        if (sessionId == null) return;
        Entry entry = getEntry(sessionId);
        putEntry(sessionId, entry.state(), lastActivityNs, entry.gaps());
    }

    public synchronized boolean hasEntry(String sessionId) {
        return sessionId != null && prefs.contains(sessionId);
    }

    public synchronized void remove(String sessionId) {
        if (sessionId != null) {
            prefs.edit().remove(sessionId).commit();
        }
    }

    public synchronized Set<String> allSessionIds() {
        return prefs.getAll().keySet();
    }

    public synchronized boolean anyActive() {
        for (String sessionId : allSessionIds()) {
            if (getState(sessionId) == Sm1State.ACTIVE) {
                return true;
            }
        }
        return false;
    }
}

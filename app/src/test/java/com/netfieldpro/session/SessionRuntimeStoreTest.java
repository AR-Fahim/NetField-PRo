package com.netfieldpro.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.core.session.Sm1State;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionRuntimeStoreTest {

    private SessionRuntimeStore store;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(SessionRuntimeStore.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        store = new SessionRuntimeStore(prefs);
    }

    @Test
    public void testSL1_storeRoundTrip_cap500Gaps_missingIsEnded() {
        // 1. Missing entry is equivalent to ENDED
        assertEquals(Sm1State.ENDED, store.getState("missing_session"));
        assertFalse(store.hasEntry("missing_session"));
        SessionRuntimeStore.Entry missingEntry = store.getEntry("missing_session");
        assertEquals(Sm1State.ENDED, missingEntry.state());
        assertEquals(0L, missingEntry.lastActivityNs());
        assertTrue(missingEntry.gaps().isEmpty());

        // 2. Round-trip
        String sid = "session-123";
        store.putEntry(sid, Sm1State.READY, 123456789L, List.of(
                new SessionRuntimeStore.Gap("SCREEN_OFF", 1000L),
                new SessionRuntimeStore.Gap("SCREEN_ON", 2000L)
        ));
        assertTrue(store.hasEntry(sid));
        assertEquals(Sm1State.READY, store.getState(sid));
        SessionRuntimeStore.Entry loaded = store.getEntry(sid);
        assertEquals(Sm1State.READY, loaded.state());
        assertEquals(123456789L, loaded.lastActivityNs());
        assertEquals(2, loaded.gaps().size());
        assertEquals("SCREEN_OFF", loaded.gaps().get(0).kind());
        assertEquals(1000L, loaded.gaps().get(0).tNs());
        assertEquals("SCREEN_ON", loaded.gaps().get(1).kind());
        assertEquals(2000L, loaded.gaps().get(1).tNs());

        // 3. Cap 500 gaps (append 505 gaps -> only newest 500 remain)
        String sid2 = "session-gaps";
        store.putState(sid2, Sm1State.ACTIVE);
        for (int i = 0; i < 505; i++) {
            store.appendGap(sid2, "SCREEN_OFF", i * 1000L);
        }
        SessionRuntimeStore.Entry gapEntry = store.getEntry(sid2);
        assertEquals(500, gapEntry.gaps().size());
        // First gap in the list should be the 5th gap added (tNs = 5000L)
        assertEquals(5000L, gapEntry.gaps().get(0).tNs());
        assertEquals(504000L, gapEntry.gaps().get(499).tNs());

        // 4. Remove session -> becomes ENDED
        store.remove(sid2);
        assertFalse(store.hasEntry(sid2));
        assertEquals(Sm1State.ENDED, store.getState(sid2));
    }
}

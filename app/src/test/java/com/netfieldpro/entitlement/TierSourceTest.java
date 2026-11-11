package com.netfieldpro.entitlement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.core.entitlement.Tier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TierSourceTest {

    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(PrefsTierSource.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testTS1_debugNotAllowedReturnsFree() {
        prefs.edit().putString(PrefsTierSource.KEY_DEBUG_TIER, "ENTERPRISE").commit();
        PrefsTierSource source = new PrefsTierSource(prefs, false);
        assertEquals(Tier.FREE, source.current());
    }

    @Test
    public void testTS2_debugAllowedStoredPro() {
        prefs.edit().putString(PrefsTierSource.KEY_DEBUG_TIER, "PRO").commit();
        PrefsTierSource source = new PrefsTierSource(prefs, true);
        assertEquals(Tier.PRO, source.current());
    }

    @Test
    public void testTS3_debugAllowedStoredGold() {
        prefs.edit().putString(PrefsTierSource.KEY_DEBUG_TIER, "GOLD").commit();
        PrefsTierSource source = new PrefsTierSource(prefs, true);
        assertEquals(Tier.FREE, source.current());
    }

    @Test
    public void testTS4_nothingStoredReturnsFree() {
        PrefsTierSource source = new PrefsTierSource(prefs, true);
        assertEquals(Tier.FREE, source.current());
    }

    @Test
    public void testTS5_setDebugTierDebugNotAllowedThrowsIse() {
        PrefsTierSource source = new PrefsTierSource(prefs, false);
        assertThrows(IllegalStateException.class, () -> source.setDebugTier(Tier.PRO));
    }

    @Test
    public void testTS6_setDebugTierPostsToLiveData() {
        PrefsTierSource source = new PrefsTierSource(prefs, true);
        EntitlementRepository repo = new EntitlementRepository(source);
        List<Tier> observed = new ArrayList<>();
        Observer<Tier> observer = observed::add;
        repo.tier().observeForever(observer);

        try {
            assertEquals(Tier.FREE, repo.current());
            repo.setDebugTier(Tier.PRO);
            assertEquals(Tier.PRO, repo.current());
            assertEquals(List.of(Tier.FREE, Tier.PRO), observed);
        } finally {
            repo.tier().removeObserver(observer);
        }
    }

    @Test
    public void testTS7_manifestAndBackupExclusions() throws Exception {
        File current = new File(".").getAbsoluteFile();
        File manifestFile = new File(current, "src/main/AndroidManifest.xml");
        if (!manifestFile.exists()) {
            manifestFile = new File(current, "app/src/main/AndroidManifest.xml");
        }
        assertTrue("Manifest must exist", manifestFile.exists());
        String manifestContent = new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("Manifest references dataExtractionRules",
                manifestContent.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""));
        assertTrue("Manifest references fullBackupContent",
                manifestContent.contains("android:fullBackupContent=\"@xml/backup_rules\""));

        File resXmlDir = new File(manifestFile.getParentFile(), "res/xml");
        File dataRulesFile = new File(resXmlDir, "data_extraction_rules.xml");
        File backupRulesFile = new File(resXmlDir, "backup_rules.xml");

        assertTrue("data_extraction_rules.xml must exist", dataRulesFile.exists());
        assertTrue("backup_rules.xml must exist", backupRulesFile.exists());

        String dataRulesContent = new String(Files.readAllBytes(dataRulesFile.toPath()), StandardCharsets.UTF_8);
        String backupRulesContent = new String(Files.readAllBytes(backupRulesFile.toPath()), StandardCharsets.UTF_8);

        assertTrue("data_extraction_rules contains nf_entitlement.xml",
                dataRulesContent.contains("nf_entitlement.xml"));
        assertTrue("backup_rules contains nf_entitlement.xml",
                backupRulesContent.contains("nf_entitlement.xml"));
    }
}

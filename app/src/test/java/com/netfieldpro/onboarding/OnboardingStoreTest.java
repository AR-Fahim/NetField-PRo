package com.netfieldpro.onboarding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.core.permissions.PermissionKey;
import com.netfieldpro.tokens.TokensTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

@RunWith(RobolectricTestRunner.class)
public class OnboardingStoreTest {

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(OnboardingStore.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testOB1_freshStoreHasDefaultsFalse() {
        OnboardingStore store = new OnboardingStore(prefs, true);
        assertFalse(store.isDone());
        assertFalse(store.wasAsked(PermissionKey.LOCATION));
        assertFalse(store.wasAsked(PermissionKey.NOTIFICATIONS));
    }

    @Test
    public void testOB2_markDonePersistsAcrossInstances() {
        OnboardingStore store1 = new OnboardingStore(prefs, true);
        store1.markDone();
        assertTrue(store1.isDone());

        OnboardingStore store2 = new OnboardingStore(prefs, true);
        assertTrue(store2.isDone());
    }

    @Test
    public void testOB3_markAskedLocationLeavesNotificationsUnset() {
        OnboardingStore store = new OnboardingStore(prefs, true);
        store.markAsked(PermissionKey.LOCATION);
        assertTrue(store.wasAsked(PermissionKey.LOCATION));
        assertFalse(store.wasAsked(PermissionKey.NOTIFICATIONS));
    }

    @Test
    public void testOB4_resetWithAllowedClearsAllThree() {
        OnboardingStore store = new OnboardingStore(prefs, true);
        store.markDone();
        store.markAsked(PermissionKey.LOCATION);
        store.markAsked(PermissionKey.NOTIFICATIONS);

        store.reset();

        assertFalse(store.isDone());
        assertFalse(store.wasAsked(PermissionKey.LOCATION));
        assertFalse(store.wasAsked(PermissionKey.NOTIFICATIONS));
    }

    @Test
    public void testOB5_resetWithoutAllowedThrowsAndPreservesValues() {
        OnboardingStore store = new OnboardingStore(prefs, false);
        store.markDone();
        store.markAsked(PermissionKey.LOCATION);
        store.markAsked(PermissionKey.NOTIFICATIONS);

        assertThrows(IllegalStateException.class, store::reset);

        assertTrue(store.isDone());
        assertTrue(store.wasAsked(PermissionKey.LOCATION));
        assertTrue(store.wasAsked(PermissionKey.NOTIFICATIONS));
    }

    @Test
    public void testOB6_fileAndKeyStringsMatchSpec() {
        assertEquals("nf_onboarding", OnboardingStore.PREFS_NAME);
        assertEquals("onboarding_done", OnboardingStore.KEY_ONBOARDING_DONE);
        assertEquals("asked_location", OnboardingStore.KEY_ASKED_LOCATION);
        assertEquals("asked_notifications", OnboardingStore.KEY_ASKED_NOTIFICATIONS);
    }

    @Test
    public void testOB7_backupXmlsExcludeOnboardingAndEntitlement() throws Exception {
        File dataExtractionFile = TokensTest.findFile("src/main/res/xml/data_extraction_rules.xml");
        File backupRulesFile = TokensTest.findFile("src/main/res/xml/backup_rules.xml");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        Document docData = dbf.newDocumentBuilder().parse(dataExtractionFile);
        NodeList excludesData = docData.getElementsByTagName("exclude");
        Set<String> dataExcluded = new HashSet<>();
        for (int i = 0; i < excludesData.getLength(); i++) {
            Element el = (Element) excludesData.item(i);
            dataExcluded.add(el.getAttribute("path"));
        }
        assertTrue("data_extraction_rules missing nf_onboarding.xml", dataExcluded.contains("nf_onboarding.xml"));
        assertTrue("data_extraction_rules missing nf_entitlement.xml", dataExcluded.contains("nf_entitlement.xml"));

        Document docBackup = dbf.newDocumentBuilder().parse(backupRulesFile);
        NodeList excludesBackup = docBackup.getElementsByTagName("exclude");
        Set<String> backupExcluded = new HashSet<>();
        for (int i = 0; i < excludesBackup.getLength(); i++) {
            Element el = (Element) excludesBackup.item(i);
            backupExcluded.add(el.getAttribute("path"));
        }
        assertTrue("backup_rules missing nf_onboarding.xml", backupExcluded.contains("nf_onboarding.xml"));
        assertTrue("backup_rules missing nf_entitlement.xml", backupExcluded.contains("nf_entitlement.xml"));
    }

    @Test
    public void testOB8_manifestPermissionsEqualInventoryPlusThreeNew() throws Exception {
        File manifestFile = TokensTest.findFile("src/main/AndroidManifest.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile);
        NodeList perms = doc.getElementsByTagName("uses-permission");
        Set<String> permSet = new HashSet<>();
        for (int i = 0; i < perms.getLength(); i++) {
            Element el = (Element) perms.item(i);
            permSet.add(el.getAttribute("android:name"));
        }

        Set<String> expected = new HashSet<>();
        expected.add("android.permission.ACCESS_FINE_LOCATION");
        expected.add("android.permission.ACCESS_COARSE_LOCATION");
        expected.add("android.permission.POST_NOTIFICATIONS");
        expected.add("android.permission.ACCESS_WIFI_STATE");
        expected.add("android.permission.CHANGE_WIFI_STATE");
        expected.add("android.permission.ACCESS_NETWORK_STATE");
        expected.add("android.permission.INTERNET");
        expected.add("android.permission.FOREGROUND_SERVICE");
        expected.add("android.permission.FOREGROUND_SERVICE_LOCATION");
        expected.add("android.permission.WAKE_LOCK");

        assertEquals(expected, permSet);
    }
}


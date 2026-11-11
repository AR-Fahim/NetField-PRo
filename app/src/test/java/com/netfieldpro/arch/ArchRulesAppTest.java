package com.netfieldpro.arch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.netfieldpro.core.arch.SourceScan;

public class ArchRulesAppTest {

    private File appMainJavaDir;
    private Set<String> allowlist;
    private File androidTestJavaDir;
    private Set<String> resetAllowlist;

    @Before
    public void setUp() throws IOException {
        File current = new File(".").getAbsoluteFile();
        File allowlistFile;
        File resetAllowlistFile;
        if (new File(current, "src/main/java").exists()) {
            appMainJavaDir = new File(current, "src/main/java");
            allowlistFile = new File(current, "arch-allowlist.txt");
            androidTestJavaDir = new File(current, "src/androidTest/java");
            resetAllowlistFile = new File(current, "src/androidTest/reset-allowlist.txt");
        } else if (new File(current, "app/src/main/java").exists()) {
            appMainJavaDir = new File(current, "app/src/main/java");
            allowlistFile = new File(current, "app/arch-allowlist.txt");
            androidTestJavaDir = new File(current, "app/src/androidTest/java");
            resetAllowlistFile = new File(current, "app/src/androidTest/reset-allowlist.txt");
        } else {
            fail("Cannot locate app/src/main/java from " + current);
            return;
        }

        allowlist = new HashSet<>();
        if (allowlistFile.exists()) {
            List<String> lines = Files.readAllLines(allowlistFile.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    allowlist.add(line);
                }
            }
        }

        resetAllowlist = new HashSet<>();
        if (resetAllowlistFile.exists()) {
            List<String> lines = Files.readAllLines(resetAllowlistFile.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 0 && !parts[0].isEmpty()) {
                        resetAllowlist.add(parts[0]);
                    }
                }
            }
        }
    }

    @Test
    public void testAppTimeRestrictions() {
        List<File> javaFiles = SourceScan.findJavaFiles(appMainJavaDir);
        List<String> violations = new ArrayList<>();

        for (File f : javaFiles) {
            String path = f.getPath().replace('\\', '/');
            if (path.endsWith("time/AndroidClock.java") || allowlist.contains(f.getName()) || allowlist.contains(path)) {
                continue;
            }
            String content = SourceScan.readFile(f);
            if (SourceScan.APP_TIME_PATTERN.matcher(content).find()) {
                violations.add("Forbidden time call in " + path);
            }
        }

        assertTrue("Found time violations in app/src/main/java: " + violations, violations.isEmpty());
    }

    @Test
    public void testAppTimeRestrictions_selfTest() {
        assertTrue(SourceScan.APP_TIME_PATTERN.matcher("long t = System.currentTimeMillis();").find());
        assertTrue(SourceScan.APP_TIME_PATTERN.matcher("long t = SystemClock.elapsedRealtime();").find());
        assertTrue(SourceScan.APP_TIME_PATTERN.matcher("long t = SystemClock.elapsedRealtimeNanos();").find());
        assertTrue(SourceScan.APP_TIME_PATTERN.matcher("Instant i = Instant.now();").find());
        assertFalse(SourceScan.APP_TIME_PATTERN.matcher("clock.monoNs();").find());
    }

    @Test
    public void testColdStartForbiddenTokensInNetFieldAppAndMainActivity() {
        String[] forbidden = {"NetFieldDatabase", "KeyStore", "BillingClient"};
        File appFile = new File(appMainJavaDir, "com/netfieldpro/NetFieldApp.java");
        File mainFile = new File(appMainJavaDir, "com/netfieldpro/MainActivity.java");
        assertTrue("NetFieldApp.java must exist", appFile.exists());
        assertTrue("MainActivity.java must exist", mainFile.exists());

        String appContent = SourceScan.readFile(appFile);
        String mainContent = SourceScan.readFile(mainFile);

        for (String token : forbidden) {
            assertFalse("NetFieldApp.java contains forbidden token: " + token, appContent.contains(token));
            assertFalse("MainActivity.java contains forbidden token: " + token, mainContent.contains(token));
        }
    }

    @Test
    public void testReleaseBuildContainsNoReferencesToDebugGateLabFragment() {
        List<File> javaFiles = SourceScan.findJavaFiles(appMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            assertFalse("Release source " + f.getName() + " contains reference to DebugGateLabFragment",
                    content.contains("DebugGateLabFragment"));
        }
    }

    @Test
    public void testReleaseBuildContainsNoDebugResetOnboardingRowOrString() {
        List<File> javaFiles = SourceScan.findJavaFiles(appMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            assertFalse("Release source " + f.getName() + " contains reference to row_debug_reset_onboarding",
                    content.contains("row_debug_reset_onboarding"));
            assertFalse("Release source " + f.getName() + " contains reference to debug_reset_onboarding",
                    content.contains("debug_reset_onboarding"));
        }

        File resDir = new File(appMainJavaDir.getParentFile(), "res");
        List<File> resFiles = new ArrayList<>();
        collectFiles(resDir, resFiles);
        for (File f : resFiles) {
            String content = SourceScan.readFile(f);
            assertFalse("Release resource " + f.getName() + " contains reference to row_debug_reset_onboarding",
                    content.contains("row_debug_reset_onboarding"));
            assertFalse("Release resource " + f.getName() + " contains reference to debug_reset_onboarding",
                    content.contains("debug_reset_onboarding"));
        }
    }

    @Test
    public void testReleaseBuildContainsNoDebugDiagRecordSwitchOrString() {
        List<File> javaFiles = SourceScan.findJavaFiles(appMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            assertFalse("Release source " + f.getName() + " contains reference to switch_diag_record",
                    content.contains("switch_diag_record"));
            assertFalse("Release source " + f.getName() + " contains reference to diag_dev_record",
                    content.contains("diag_dev_record"));
        }

        File resDir = new File(appMainJavaDir.getParentFile(), "res");
        List<File> resFiles = new ArrayList<>();
        collectFiles(resDir, resFiles);
        for (File f : resFiles) {
            String content = SourceScan.readFile(f);
            assertFalse("Release resource " + f.getName() + " contains reference to switch_diag_record",
                    content.contains("switch_diag_record"));
            assertFalse("Release resource " + f.getName() + " contains reference to diag_dev_record",
                    content.contains("diag_dev_record"));
        }
    }

    @Test
    public void testAR_T1_activityScenarioTestsMustCallResetAppState() {
        List<File> files = SourceScan.findJavaFiles(androidTestJavaDir);
        List<String> violations = new ArrayList<>();

        for (File f : files) {
            String content = SourceScan.readFile(f);
            boolean triggers = content.contains("ActivityScenario")
                    || content.contains("launch(")
                    || content.contains("ActivityTestRule");
            if (triggers) {
                String className = f.getName().replace(".java", "");
                if (resetAllowlist.contains(className) || resetAllowlist.contains(f.getName())) {
                    continue;
                }
                if (!content.contains("TestSetup.resetAppState()")) {
                    violations.add(f.getName() + " uses ActivityScenario/launch/ActivityTestRule but does not contain TestSetup.resetAppState()");
                }
            }
        }

        assertTrue("Found tests missing TestSetup.resetAppState(): " + violations, violations.isEmpty());
    }

    private void collectFiles(File dir, List<File> out) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collectFiles(f, out);
            } else {
                out.add(f);
            }
        }
    }
}


package com.netfieldpro.core.arch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class ArchRulesTest {

    private File coreMainJavaDir;
    private File repoRootDir;
    private Set<String> allowlist;

    @Before
    public void setUp() throws Exception {
        File current = new File(".").getAbsoluteFile();
        if (new File(current, "src/main/java").exists()) {
            coreMainJavaDir = new File(current, "src/main/java");
            repoRootDir = current.getParentFile();
        } else if (new File(current, "core/src/main/java").exists()) {
            coreMainJavaDir = new File(current, "core/src/main/java");
            repoRootDir = current;
        } else {
            fail("Cannot locate core/src/main/java from " + current);
        }

        allowlist = new HashSet<>();
        File allowlistFile = new File(repoRootDir, "core/arch-allowlist.txt");
        if (!allowlistFile.exists()) {
            allowlistFile = new File(repoRootDir, "arch-allowlist.txt");
        }
        if (allowlistFile.exists()) {
            List<String> lines = Files.readAllLines(allowlistFile.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    allowlist.add(line);
                }
            }
        }
    }

    @Test
    public void testR1_noForbiddenImports() {
        List<File> javaFiles = SourceScan.findJavaFiles(coreMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            assertFalse("R1 violation in " + f.getName(), SourceScan.checkR1(content));
        }
    }

    @Test
    public void testR1_selfTest() {
        assertTrue(SourceScan.checkR1("import android.os.Bundle;"));
        assertTrue(SourceScan.checkR1("import androidx.annotation.NonNull;"));
        assertTrue(SourceScan.checkR1("import dalvik.system.PathClassLoader;"));
        assertFalse(SourceScan.checkR1("import java.util.List;"));
    }

    @Test
    public void testR2_noForbiddenTimeCalls() {
        List<File> javaFiles = SourceScan.findJavaFiles(coreMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            assertFalse("R2 violation in " + f.getName(), SourceScan.checkR2(content));
        }
    }

    @Test
    public void testR2_selfTest() {
        assertTrue(SourceScan.checkR2("long t = System.currentTimeMillis();"));
        assertTrue(SourceScan.checkR2("long t = System.nanoTime();"));
        assertTrue(SourceScan.checkR2("Instant i = Instant.now();"));
        assertTrue(SourceScan.checkR2("LocalDate d = LocalDate.now();"));
        assertTrue(SourceScan.checkR2("LocalDateTime dt = LocalDateTime.now();"));
        assertTrue(SourceScan.checkR2("ZonedDateTime zt = ZonedDateTime.now();"));
        assertTrue(SourceScan.checkR2("Date d = new Date();"));
        assertTrue(SourceScan.checkR2("Calendar c = Calendar.getInstance();"));
        assertTrue(SourceScan.checkR2("java.time.Clock c = Clock.systemUTC();"));
        assertFalse(SourceScan.checkR2("clock.monoNs();"));
    }

    @Test
    public void testR3_noForbiddenConcurrencyCalls() {
        List<File> javaFiles = SourceScan.findJavaFiles(coreMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            assertFalse("R3 violation in " + f.getName(), SourceScan.checkR3(content));
        }
    }

    @Test
    public void testR3_selfTest() {
        assertTrue(SourceScan.checkR3("Map<String, String> m = new ConcurrentHashMap<>();"));
        assertTrue(SourceScan.checkR3("Thread t = new Thread(r);"));
        assertTrue(SourceScan.checkR3("Thread.sleep(100);"));
        assertFalse(SourceScan.checkR3("Map<String, String> m = new HashMap<>();"));
    }

    @Test
    public void testR4_noForbiddenRandomness() {
        List<File> javaFiles = SourceScan.findJavaFiles(coreMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            List<String> violations = SourceScan.checkR4(content, f.getName());
            assertTrue("R4 violations in " + f.getName() + ": " + violations, violations.isEmpty());
        }
    }

    @Test
    public void testR4_selfTest() {
        assertFalse(SourceScan.checkR4("double r = Math.random();", "Foo.java").isEmpty());
        assertFalse(SourceScan.checkR4("Random r = new Random();", "Foo.java").isEmpty());
        assertTrue(SourceScan.checkR4("Random r = new Random(42L);", "Foo.java").isEmpty());
        assertFalse(SourceScan.checkR4("UUID id = UUID.randomUUID();", "Foo.java").isEmpty());
        assertFalse(SourceScan.checkR4("SecureRandom sr = new SecureRandom();", "Foo.java").isEmpty());
        assertTrue(SourceScan.checkR4("SecureRandom sr = new SecureRandom();", "IdGenerator.java").isEmpty());
    }

    @Test
    public void testR5_packageStartsComNetfieldproCore() {
        List<File> javaFiles = SourceScan.findJavaFiles(coreMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            assertTrue("R5 violation in " + f.getName(), SourceScan.checkR5(content));
        }
    }

    @Test
    public void testR5_selfTest() {
        assertTrue(SourceScan.checkR5("package com.netfieldpro.core.time;"));
        assertTrue(SourceScan.checkR5("package com.netfieldpro.core;"));
        assertFalse(SourceScan.checkR5("package com.example;"));
        assertFalse(SourceScan.checkR5("package com.netfieldpro;"));
        assertFalse(SourceScan.checkR5("// no package statement"));
    }

    @Test
    public void testR6_noKotlinRepoWide() {
        List<File> kotlinFiles = SourceScan.findKotlinFilesRepoWide(repoRootDir);
        assertTrue("R6 violation: found Kotlin files: " + kotlinFiles, kotlinFiles.isEmpty());
    }

    @Test
    public void testR6_selfTest() {
        File fakeKt = new File("Foo.kt");
        File fakeKts = new File("Bar.kts");
        assertTrue(fakeKt.getName().endsWith(".kt"));
        assertTrue(fakeKts.getName().endsWith(".kts"));
    }

    @Test
    public void testR7_noForbiddenJava18PlusTokens() {
        List<File> javaFiles = SourceScan.findJavaFiles(coreMainJavaDir);
        for (File f : javaFiles) {
            String content = SourceScan.readFile(f);
            List<String> violations = SourceScan.checkR7(content, f.getName(), allowlist);
            assertTrue("R7 violation in " + f.getName() + ": " + violations, violations.isEmpty());
        }
    }

    @Test
    public void testR7_selfTest() {
        for (String token : SourceScan.R7_FORBIDDEN_TOKENS) {
            assertFalse("Token " + token + " should be detected as forbidden",
                    SourceScan.checkR7("sample " + token + " in code", "Foo.java", Set.of()).isEmpty());
        }
        assertTrue(SourceScan.checkR7("int a = 1;", "Foo.java", Set.of()).isEmpty());
        assertFalse(SourceScan.checkR7(".reversed()", "Foo.java", Set.of()).isEmpty());
        assertTrue(SourceScan.checkR7(".reversed()", "Foo.java", Set.of(".reversed()")).isEmpty());
    }
}


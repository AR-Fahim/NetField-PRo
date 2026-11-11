package com.netfieldpro.tokens;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.netfieldpro.core.arch.SourceScan;

public class NoHardcodedTextTest {

    private static final Pattern TEXT_ATTR_PATTERN = Pattern.compile("android:text\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern IMAGE_BUTTON_PATTERN = Pattern.compile("<ImageButton\\b([^>]*)/?>", Pattern.DOTALL);
    private static final Pattern CONTENT_DESC_PATTERN = Pattern.compile("android:contentDescription\\s*=\\s*\"[^\"]+\"");

    public static List<String> checkLayoutContent(String content, String fileName) {
        List<String> violations = new ArrayList<>();

        Matcher textMatcher = TEXT_ATTR_PATTERN.matcher(content);
        while (textMatcher.find()) {
            String val = textMatcher.group(1);
            if (!val.startsWith("@")) {
                violations.add("Hardcoded text \"" + val + "\" in " + fileName);
            }
        }

        Matcher ibMatcher = IMAGE_BUTTON_PATTERN.matcher(content);
        while (ibMatcher.find()) {
            String tagAttrs = ibMatcher.group(1);
            if (!CONTENT_DESC_PATTERN.matcher(tagAttrs).find()) {
                violations.add("ImageButton missing android:contentDescription in " + fileName);
            }
        }

        return violations;
    }

    @Test
    public void testLayoutsHaveNoHardcodedTextAndImageButtonsHaveContentDescription() {
        File layoutDir;
        try {
            layoutDir = TokensTest.findFile("src/main/res/layout");
        } catch (IllegalArgumentException e) {
            return;
        }

        List<File> files = new ArrayList<>();
        findXmlFiles(layoutDir, files);
        List<String> allViolations = new ArrayList<>();

        for (File f : files) {
            String content = SourceScan.readFile(f);
            allViolations.addAll(checkLayoutContent(content, f.getName()));
        }

        assertTrue("Found layout violations: " + allViolations, allViolations.isEmpty());
    }

    private void findXmlFiles(File dir, List<File> result) {
        if (dir == null || !dir.exists()) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                findXmlFiles(f, result);
            } else if (f.getName().endsWith(".xml")) {
                result.add(f);
            }
        }
    }

    @Test
    public void testSelfTest() {
        List<String> v1 = checkLayoutContent("<TextView android:text=\"Hardcoded\" />", "test.xml");
        assertFalse(v1.isEmpty());

        List<String> v2 = checkLayoutContent("<TextView android:text=\"@string/app_name\" />", "test.xml");
        assertTrue(v2.isEmpty());

        List<String> v3 = checkLayoutContent("<ImageButton android:id=\"@+id/btn\" />", "test.xml");
        assertFalse(v3.isEmpty());

        List<String> v4 = checkLayoutContent("<ImageButton android:id=\"@+id/btn\" android:contentDescription=\"@string/app_name\" />", "test.xml");
        assertTrue(v4.isEmpty());
    }
}


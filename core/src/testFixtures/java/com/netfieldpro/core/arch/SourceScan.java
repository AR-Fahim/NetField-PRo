package com.netfieldpro.core.arch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceScan {

    public static final Pattern R1_PATTERN = Pattern.compile("import\\s+(android\\.|androidx\\.|dalvik\\.)");

    public static final Pattern R2_PATTERN = Pattern.compile(
            "(\\bSystem\\.(currentTimeMillis|nanoTime)\\b|" +
            "\\b(Instant|LocalDate|LocalDateTime|ZonedDateTime)\\.now\\b|" +
            "\\bnew\\s+Date\\s*\\(|" +
            "\\bCalendar\\.getInstance\\b|" +
            "\\bClock\\.system)"
    );

    public static final Pattern R3_PATTERN = Pattern.compile(
            "(\\bConcurrentHashMap\\b|\\bnew\\s+Thread\\s*\\(|\\bThread\\.sleep\\b)"
    );

    public static final Pattern R4_MATH_RANDOM = Pattern.compile("\\bMath\\.random\\b");
    public static final Pattern R4_UNSEEDED_RANDOM = Pattern.compile("\\bnew\\s+Random\\s*\\(\\s*\\)");
    public static final Pattern R4_UUID = Pattern.compile("\\bUUID\\.randomUUID\\b");
    public static final Pattern R4_SECURE_RANDOM = Pattern.compile("\\bSecureRandom\\b");

    public static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");

    public static final Pattern APP_TIME_PATTERN = Pattern.compile(
            "(\\bSystem\\.(currentTimeMillis|nanoTime)\\b|" +
            "\\b(Instant|LocalDate|LocalDateTime|ZonedDateTime)\\.now\\b|" +
            "\\bnew\\s+Date\\s*\\(|" +
            "\\bCalendar\\.getInstance\\b|" +
            "\\bClock\\.system|" +
            "\\bSystemClock\\.elapsedRealtime)"
    );

    public static final List<String> R7_FORBIDDEN_TOKENS = List.of(
            ".reversed()",
            "HexFormat",
            "Math.clamp(",
            ".toList()",
            ".formatted(",
            ".repeat(",
            ".isBlank()",
            ".strip()",
            ".lines()",
            ".takeWhile(",
            ".dropWhile(",
            ".mapMulti(",
            "SequencedCollection",
            "SequencedMap",
            "SequencedSet"
    );

    public static List<File> findJavaFiles(File dir) {
        List<File> result = new ArrayList<>();
        if (dir == null || !dir.exists()) {
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                result.addAll(findJavaFiles(f));
            } else if (f.getName().endsWith(".java")) {
                result.add(f);
            }
        }
        return result;
    }

    public static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getAbsolutePath(), e);
        }
    }

    public static boolean checkR1(String content) {
        return R1_PATTERN.matcher(content).find();
    }

    public static boolean checkR2(String content) {
        return R2_PATTERN.matcher(content).find();
    }

    public static boolean checkR3(String content) {
        return R3_PATTERN.matcher(content).find();
    }

    public static List<String> checkR4(String content, String fileName) {
        List<String> violations = new ArrayList<>();
        if (R4_MATH_RANDOM.matcher(content).find()) {
            violations.add("Math.random");
        }
        if (R4_UNSEEDED_RANDOM.matcher(content).find()) {
            violations.add("unseeded new Random()");
        }
        if (R4_UUID.matcher(content).find()) {
            violations.add("UUID.randomUUID");
        }
        if (R4_SECURE_RANDOM.matcher(content).find()) {
            if (!"IdGenerator.java".equals(fileName)) {
                violations.add("SecureRandom outside IdGenerator.java");
            }
        }
        return violations;
    }

    public static List<String> checkR7(String content, String fileName, java.util.Set<String> allowlist) {
        List<String> violations = new ArrayList<>();
        for (String token : R7_FORBIDDEN_TOKENS) {
            if (content.contains(token)) {
                if (allowlist != null && (allowlist.contains(token) || allowlist.contains(fileName + ":" + token) || allowlist.contains(fileName))) {
                    continue;
                }
                violations.add(token);
            }
        }
        return violations;
    }

    public static boolean checkR5(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        if (!matcher.find()) {
            return false;
        }
        String pkg = matcher.group(1);
        return pkg.equals("com.netfieldpro.core") || pkg.startsWith("com.netfieldpro.core.");
    }

    public static List<File> findKotlinFilesRepoWide(File repoRoot) {
        List<File> violations = new ArrayList<>();
        scanRepoForKotlin(repoRoot, violations);
        return violations;
    }

    private static void scanRepoForKotlin(File file, List<File> violations) {
        if (file == null || !file.exists()) {
            return;
        }
        String name = file.getName();
        if (file.isDirectory()) {
            if (name.equals("build") || name.equals(".gradle") || name.equals(".git")) {
                return;
            }
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    scanRepoForKotlin(child, violations);
                }
            }
        } else {
            if (name.endsWith(".kt") || name.endsWith(".kts")) {
                violations.add(file);
            }
        }
    }
}


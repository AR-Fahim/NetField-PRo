import os, re, glob, subprocess

def run_cmd(cmd):
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def grep_files(pattern, dirs, max_hits=5, extensions=('.java', '.xml', '.gradle')):
    hits = []
    regex = re.compile(pattern)
    for d in dirs:
        for root, _, files in os.walk(d):
            for f in files:
                if any(f.endswith(ext) for ext in extensions):
                    path = os.path.join(root, f).replace('\\', '/')
                    try:
                        with open(path, 'r', encoding='utf-8', errors='ignore') as fp:
                            for idx, line in enumerate(fp, 1):
                                if regex.search(line):
                                    hits.append(f"{path}:{idx}: {line.strip()}")
                                    if len(hits) >= max_hits:
                                        return hits
                    except Exception:
                        pass
    return hits if hits else ["NONE FOUND"]

def collect_facts():
    lines = []
    lines.append("# NetField Pro Facts\n")

    # F-1 Counts
    lines.append("## F-1 File and Line Counts\n```")
    dirs = ['core/src/main', 'app/src/main', 'app/src/gated', 'app/src/open', 'core/src/test', 'app/src/test']
    for d in dirs:
        files = glob.glob(d + '/**/*.java', recursive=True)
        num_lines = sum(len(open(f, encoding='utf-8', errors='ignore').readlines()) for f in files)
        lines.append(f"{d}: {len(files)} files, {num_lines} lines")
    lines.append("```\n")

    # F-2 Tests
    lines.append("## F-2 Tests and Verification\n```")
    test_counts = run_cmd("python scripts/test-counts.py")
    lines.append(test_counts)
    vector_verify = run_cmd("python tools/verify_vectors.py")
    lines.append(vector_verify.splitlines()[-1] if vector_verify else "")
    vector_selftest = run_cmd("python tools/verify_vectors.py --selftest")
    lines.append(vector_selftest.splitlines()[-1] if vector_selftest else "")
    auth_iso = run_cmd("python scripts/check-auth-isolation.py")
    lines.append(auth_iso)
    lines.append("```\n")

    # F-3 Manifest
    lines.append("## F-3 Manifest\n")
    lines.append("### Source Manifest (`app/src/main/AndroidManifest.xml`)\n```xml")
    with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
        lines.append(f.read().strip())
    lines.append("```\n")

    merged = glob.glob('app/build/intermediates/merged_manifests/gatedDebug/**/AndroidManifest.xml', recursive=True)
    if merged:
        lines.append(f"### Merged Manifest (`{merged[0].replace(os.sep, '/')}`)\n```xml")
        with open(merged[0], 'r', encoding='utf-8') as f:
            lines.append(f.read().strip())
        lines.append("```\n")

    # F-4 Concept Greps
    lines.append("## F-4 Concept Greps\n")

    fb_target = "com.google." + "firebase"
    concepts = [
        ("Activities (`extends AppCompatActivity|extends Activity`)",
         r'extends\s+(?:AppCompatActivity|Activity)\b', ['app/src', 'core/src']),
        ("Explicit intents (`new Intent(.*.class` and `PendingIntent`)",
         r'new\s+Intent\([^)]*\.class|PendingIntent', ['app/src', 'core/src']),
        ("Implicit intents (`ACTION_SEND|ACTION_VIEW|ACTION_OPEN_DOCUMENT|ACTION_GET_CONTENT|Settings.ACTION_|createChooser`)",
         r'ACTION_SEND|ACTION_VIEW|ACTION_OPEN_DOCUMENT|ACTION_GET_CONTENT|Settings\.ACTION_|createChooser', ['app/src', 'core/src']),
        ("ViewBinding (`Binding.inflate`)",
         r'Binding\.inflate', ['app/src']),
        ("Event handling (`setOnClickListener|OnTouchListener|ScaleGestureDetector|GestureDetector|postDelayed|OnLongClick|setOnEditorActionListener`)",
         r'setOnClickListener|OnTouchListener|ScaleGestureDetector|GestureDetector|postDelayed|OnLongClick|setOnEditorActionListener', ['app/src']),
        ("Receivers (`BroadcastReceiver|registerReceiver|ACTION_BATTERY_CHANGED|ACTION_BATTERY_LOW|ACTION_BATTERY_OKAY|AIRPLANE_MODE|SCAN_RESULTS_AVAILABLE_ACTION`)",
         r'BroadcastReceiver|registerReceiver|ACTION_BATTERY_CHANGED|ACTION_BATTERY_LOW|ACTION_BATTERY_OKAY|AIRPLANE_MODE|SCAN_RESULTS_AVAILABLE_ACTION', ['app/src', 'core/src']),
        ("Content providers (`FileProvider|ContentResolver|DocumentsContract|MediaStore|ContactsContract|ContentProvider`)",
         r'FileProvider|ContentResolver|DocumentsContract|MediaStore|ContactsContract|ContentProvider', ['app/src', 'core/src']),
        ("Services/notifications (`extends Service|startForegroundService|startForeground|NotificationCompat|NotificationManager|setOngoing`)",
         r'extends\s+Service\b|startForegroundService|startForeground|NotificationCompat|NotificationManager|setOngoing', ['app/src', 'core/src']),
        ("Networking (`HttpURLConnection|OkHttp|Retrofit|URL(|Socket|ConnectivityManager|bindSocket|InetAddress|JsonParser|CanonicalJsonWriter`)",
         r'HttpURLConnection|OkHttp|Retrofit|URL\(|Socket|ConnectivityManager|bindSocket|InetAddress|JsonParser|CanonicalJsonWriter', ['app/src', 'core/src']),
        (f"Firebase (`{fb_target}|FirebaseAuth|Firestore|FirebaseDatabase|FirebaseStorage`) under app/src/gated",
         re.escape(fb_target) + r'|FirebaseAuth|Firestore|FirebaseDatabase|FirebaseStorage', ['app/src/gated']),
        ("Firebase under app/src/main, app/src/open, core/src (Expected: NONE FOUND)",
         re.escape(fb_target) + r'|FirebaseAuth|Firestore|FirebaseDatabase|FirebaseStorage', ['app/src/main', 'app/src/open', 'core/src']),
        ("Auth hooks (`AuthSessionGateway`, `NoOpAuthGatewayImpl`, `AuthGatewayFlavor`, `StartRouter`, `nav_auth_slot`, `SyncRepository`)",
         r'AuthSessionGateway|NoOpAuthGatewayImpl|AuthGatewayFlavor|StartRouter|nav_auth_slot|SyncRepository', ['app/src', 'core/src']),
        ("Picture storage (`filesDir`, `plans/`, `cache/reports`, `BitmapFactory`, `BitmapRegionDecoder`, `Bitmap.compress`)",
         r'filesDir|plans/|cache/reports|BitmapFactory|BitmapRegionDecoder|Bitmap\.compress', ['app/src', 'core/src'])
    ]

    for title, pat, target_dirs in concepts:
        lines.append(f"### {title}\n```")
        hits = grep_files(pat, target_dirs)
        lines.extend(hits)
        lines.append("```\n")

    # ViewBinding buildFeatures
    lines.append("### ViewBinding `buildFeatures` in `app/build.gradle`\n```groovy")
    with open('app/build.gradle', 'r', encoding='utf-8') as f:
        in_bf = False
        bf_lines = []
        for l in f:
            if 'buildFeatures' in l:
                in_bf = True
            if in_bf:
                bf_lines.append(l.rstrip())
                if '}' in l:
                    break
        lines.append("\n".join(bf_lines))
    lines.append("```\n")

    # Firebase build.gradle config
    lines.append("### Firebase dependencies and productFlavors in `app/build.gradle`\n```groovy")
    with open('app/build.gradle', 'r', encoding='utf-8') as f:
        content = f.read()
        flavors = re.search(r'flavorDimensions.*?productFlavors\s*\{.*?\}\s*\}', content, re.DOTALL)
        if flavors:
            lines.append(flavors.group(0))
        deps = [l.strip() for l in content.splitlines() if 'firebase' in l]
        lines.append("\nFirebase dependencies:\n" + "\n".join(deps))
    lines.append("```\n")

    # Layout counts
    lines.append("### Layouts count in `app/src/main/res/layout`\n```")
    layout_counts = {'ConstraintLayout': 0, 'LinearLayout': 0, 'RelativeLayout': 0, 'FrameLayout': 0, 'CoordinatorLayout': 0}
    for f in glob.glob('app/src/main/res/layout/**/*.xml', recursive=True):
        c = open(f, encoding='utf-8', errors='ignore').read()
        for tag in layout_counts:
            layout_counts[tag] += len(re.findall(r'<(?:\w+\.)*' + tag + r'[\s/>]', c))
    for tag, count in layout_counts.items():
        lines.append(f"{tag}: {count}")
    lines.append("```\n")

    # RecyclerView adapters and layouts
    lines.append("### RecyclerView Adapters and Layouts\n```")
    for root, _, files in os.walk('app/src/main'):
        for f in files:
            if f.endswith('.java'):
                path = os.path.join(root, f).replace('\\', '/')
                c = open(path, encoding='utf-8', errors='ignore').read()
                if 'RecyclerView.Adapter' in c or 'ListAdapter<' in c:
                    layouts = re.findall(r'R\.layout\.([a-zA-Z0-9_]+)', c)
                    lines.append(f"Adapter: {f} ({path}) -> Layouts: {layouts}")
    lines.append("```\n")

    # Persistence: Entity, Dao, foreignKeys, NetFieldDatabase, SharedPreferences
    lines.append("### Persistence\n```")
    entities = []
    daos = []
    fk_count = 0
    for root, _, files in os.walk('app/src/main'):
        for f in files:
            if f.endswith('.java'):
                path = os.path.join(root, f).replace('\\', '/')
                c = open(path, encoding='utf-8', errors='ignore').read()
                if '@Entity' in c:
                    entities.append(f.replace('.java', ''))
                if '@Dao' in c:
                    daos.append(f.replace('.java', ''))
                fk_count += len(re.findall(r'ForeignKey\.CASCADE|foreignKeys', c))
    lines.append(f"@Entity count: {len(entities)}, Names: {entities}")
    lines.append(f"@Dao count: {len(daos)}, Names: {daos}")
    lines.append(f"foreignKeys / ForeignKey.CASCADE count: {fk_count}")

    with open('app/src/main/java/com/netfieldpro/data/db/NetFieldDatabase.java', 'r', encoding='utf-8') as f:
        for idx, l in enumerate(f, 1):
            if 'version' in l:
                lines.append(f"NetFieldDatabase.java:{idx}: {l.strip()}")

    sp_names = set()
    for root, _, files in os.walk('app/src/main'):
        for f in files:
            if f.endswith('.java'):
                path = os.path.join(root, f).replace('\\', '/')
                c = open(path, encoding='utf-8', errors='ignore').read()
                matches = re.findall(r'getSharedPreferences\(\s*\"([^\"]+)\"', c)
                sp_names.update(matches)
    lines.append(f"SharedPreferences files: {sorted(list(sp_names))}")
    lines.append("```\n")

    # Data limits in PART-B.md
    lines.append("### Data limits from `docs/freeze/PART-B.md`\n```")
    with open('docs/freeze/PART-B.md', 'r', encoding='utf-8') as f:
        for idx, l in enumerate(f, 1):
            if any(term in l for term in ['300 TAP', '1,500 scans', '300k RawSample', '20k link samples']):
                lines.append(f"docs/freeze/PART-B.md:{idx}: {l.strip()}")
    lines.append("```\n")

    # F-5 Dependencies and Arch rules
    lines.append("## F-5 Dependencies and Architecture Rules\n")
    lines.append("### `:core/build.gradle` Dependencies\n```groovy")
    with open('core/build.gradle', 'r', encoding='utf-8') as f:
        lines.append(f.read().strip())
    lines.append("```\n")

    lines.append("### `ArchRulesTest` Rules (R1–R7)\n```")
    with open('core/src/test/java/com/netfieldpro/core/arch/ArchRulesTest.java', 'r', encoding='utf-8') as f:
        for idx, l in enumerate(f, 1):
            if 'public void testR' in l:
                lines.append(f"ArchRulesTest.java:{idx}: {l.strip()}")
    lines.append("```\n")

    os.makedirs('docs/report', exist_ok=True)
    with open('docs/report/FACTS.md', 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))
    print("FACTS.md written successfully.")

if __name__ == '__main__':
    collect_facts()


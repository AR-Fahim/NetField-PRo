#!/usr/bin/env python3
import os
import re
import sys
import tempfile

def check_report(report_path):
    if not os.path.exists(report_path):
        print(f"File not found: {report_path}", file=sys.stderr)
        return 1

    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    with open(report_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    link_pattern = re.compile(r'\[.*?\]\(([^)]+)\)')

    for line in lines:
        stripped = line.strip()
        # (a) contains file:///
        if "file:///" in line:
            print(f"VIOLATION (file:///): {stripped}")
            return 1

        # (b) matches /total automated/i
        if re.search(r"total automated", line, re.IGNORECASE):
            print(f"VIOLATION (total automated): {stripped}")
            return 1

        # (c) matches /preserved from/i
        if re.search(r"preserved from", line, re.IGNORECASE):
            print(f"VIOLATION (preserved from): {stripped}")
            return 1

        # (d) links to docs/evidence/... or evidence/... paths that do not exist
        for match in link_pattern.finditer(line):
            target = match.group(1).split('#')[0].strip()
            norm_target = target.replace('\\', '/')
            if norm_target.startswith("docs/evidence/") or norm_target.startswith("evidence/") or "docs/evidence/" in norm_target:
                candidates = [
                    os.path.join(repo_root, norm_target),
                    os.path.join(os.path.dirname(os.path.abspath(report_path)), norm_target)
                ]
                if not any(os.path.exists(c) for c in candidates):
                    print(f"VIOLATION (missing evidence link '{target}'): {stripped}")
                    return 1

    return 0

def run_selftest():
    # 3 temp files (one per violation)
    with tempfile.NamedTemporaryFile("w", delete=False, suffix=".md", encoding="utf-8") as f1:
        f1.write("# Report\n[link](file:///path/to/file)\n")
        t1 = f1.name

    with tempfile.NamedTemporaryFile("w", delete=False, suffix=".md", encoding="utf-8") as f2:
        f2.write("# Report\nTotal Automated Tests: 100\n")
        t2 = f2.name

    with tempfile.NamedTemporaryFile("w", delete=False, suffix=".md", encoding="utf-8") as f3:
        f3.write("# Report\nBenchmark preserved from M3\n")
        t3 = f3.name

    with tempfile.NamedTemporaryFile("w", delete=False, suffix=".md", encoding="utf-8") as f4:
        f4.write("# Clean Report\nAll tests passed cleanly.\n")
        t4 = f4.name

    try:
        c1 = check_report(t1)
        c2 = check_report(t2)
        c3 = check_report(t3)
        c4 = check_report(t4)

        if c1 == 1 and c2 == 1 and c3 == 1 and c4 == 0:
            print("CHECK-REPORT SELFTEST OK")
            return 0
        else:
            print(f"Self-test failed: c1={c1}, c2={c2}, c3={c3}, c4={c4}", file=sys.stderr)
            return 1
    finally:
        for p in [t1, t2, t3, t4]:
            try:
                os.remove(p)
            except OSError:
                pass

def main():
    if len(sys.argv) < 2:
        print("Usage: scripts/check-report.py <report.md> | --selftest", file=sys.stderr)
        sys.exit(1)

    if sys.argv[1] == "--selftest":
        code = run_selftest()
        sys.exit(code)

    code = check_report(sys.argv[1])
    sys.exit(code)

if __name__ == "__main__":
    main()

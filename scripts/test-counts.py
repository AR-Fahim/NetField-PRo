#!/usr/bin/env python3
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

def parse_xml_file(file_path):
    tests = 0
    failures = 0
    errors = 0
    skipped = 0
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        suites = [root] if root.tag == "testsuite" else root.findall(".//testsuite")
        if not suites:
            suites = [root]
        for s in suites:
            tests += int(s.attrib.get("tests", 0))
            failures += int(s.attrib.get("failures", 0))
            errors += int(s.attrib.get("errors", 0))
            skipped += int(s.attrib.get("skipped", s.attrib.get("ignored", 0)))
    except Exception as ex:
        print(f"Warning parsing {file_path}: {ex}", file=sys.stderr)
    return tests, failures, errors, skipped

def count_pattern(file_pattern):
    files = glob.glob(file_pattern, recursive=True)
    tests = 0
    failures = 0
    errors = 0
    skipped = 0
    for f in sorted(files):
        t, fa, e, s = parse_xml_file(f)
        tests += t
        failures += fa
        errors += e
        skipped += s
    return tests, failures, errors, skipped

def get_device_label(filename):
    m = re.search(r"^TEST-(.+?)-_?app-", filename)
    if m:
        return m.group(1)
    if filename.startswith("TEST-") and "-app-" in filename:
        return filename[5:filename.find("-app-")]
    return filename

def main():
    root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    core_tests, core_fail, core_err, core_skip = count_pattern(
        os.path.join(root_dir, "core", "build", "test-results", "test", "*.xml")
    )
    app_tests, app_fail, app_err, app_skip = count_pattern(
        os.path.join(root_dir, "app", "build", "test-results", "testDebugUnitTest", "*.xml")
    )
    
    host_tests = core_tests + app_tests
    host_fail = core_fail + app_fail
    host_err = core_err + app_err
    host_skip = core_skip + app_skip
    
    print(f":core: tests {core_tests} failures {core_fail} errors {core_err} skipped {core_skip}")
    print(f":app (unit): tests {app_tests} failures {app_fail} errors {app_err} skipped {app_skip}")
    print(f"HOST TOTAL: tests {host_tests} failures {host_fail} errors {host_err} skipped {host_skip}")
    
    device_files = glob.glob(
        os.path.join(root_dir, "app", "build", "outputs", "androidTest-results", "**", "*.xml"),
        recursive=True
    )
    
    dev_failures = 0
    dev_errors = 0
    
    for f in sorted(device_files):
        basename = os.path.basename(f)
        if not basename.startswith("TEST-"):
            continue
        label = get_device_label(basename)
        t, fa, e, s = parse_xml_file(f)
        print(f":app (device {label}): tests {t} failures {fa} errors {e} skipped {s}")
        dev_failures += fa
        dev_errors += e

    if host_fail > 0 or host_err > 0 or dev_failures > 0 or dev_errors > 0:
        sys.exit(1)

if __name__ == "__main__":
    main()

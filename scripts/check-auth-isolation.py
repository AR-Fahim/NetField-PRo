#!/usr/bin/env python3
import glob
import os
import sys
import zipfile


def check_source_isolation(repo_root):
    # 1. Every file containing com.google.firebase is under app/src/gated/, and none are in core/
    for root, dirs, files in os.walk(repo_root):
        dirs[:] = [d for d in dirs if d not in ('.git', 'build', '.gradle', 'gradle')]
        for f in files:
            file_path = os.path.join(root, f)
            rel_path = os.path.relpath(file_path, repo_root).replace('\\', '/')

            if rel_path.startswith('docs/') or rel_path.startswith('scripts/') or rel_path == 'app/build.gradle':
                continue

            try:
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as fp:
                    content = fp.read()
                    if 'com.google.firebase' in content:
                        if rel_path.startswith('core/'):
                            print(f"ERROR: Forbidden reference in core: {rel_path}", file=sys.stderr)
                            sys.exit(1)
                        if not rel_path.startswith('app/src/gated/'):
                            print(f"ERROR: File outside app/src/gated/ contains com.google.firebase: {rel_path}", file=sys.stderr)
                            sys.exit(1)
            except Exception:
                pass


def check_open_apks(repo_root):
    # 2. Every app/build/outputs/apk/open/debug/*.apk has no dex containing com/google/firebase or Lcom/google/android/gms/
    open_apks = glob.glob(os.path.join(repo_root, "app", "build", "outputs", "apk", "open", "debug", "*.apk"))
    if not open_apks:
        print("ERROR: No open debug APK found", file=sys.stderr)
        sys.exit(1)

    for apk in open_apks:
        with zipfile.ZipFile(apk, 'r') as zf:
            for name in zf.namelist():
                if name.startswith('classes') and name.endswith('.dex'):
                    dex_data = zf.read(name)
                    if b'com/google/firebase' in dex_data:
                        print(f"ERROR: {apk} contains com/google/firebase in {name}", file=sys.stderr)
                        sys.exit(1)
                    if b'Lcom/google/android/gms/' in dex_data:
                        print(f"ERROR: {apk} contains Lcom/google/android/gms/ in {name}", file=sys.stderr)
                        sys.exit(1)


def check_gated_apks(repo_root):
    # 3. Sanity: the gated debug APK DOES contain com/google/firebase/auth, otherwise fail ("scan is broken")
    gated_apks = glob.glob(os.path.join(repo_root, "app", "build", "outputs", "apk", "gated", "debug", "*.apk"))
    if not gated_apks:
        print("ERROR: No gated debug APK found", file=sys.stderr)
        sys.exit(1)

    found_firebase_auth = False
    for apk in gated_apks:
        with zipfile.ZipFile(apk, 'r') as zf:
            for name in zf.namelist():
                if name.startswith('classes') and name.endswith('.dex'):
                    dex_data = zf.read(name)
                    if b'com/google/firebase/auth' in dex_data:
                        found_firebase_auth = True
                        break

    if not found_firebase_auth:
        print("ERROR: scan is broken", file=sys.stderr)
        sys.exit(1)


def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    check_source_isolation(repo_root)
    check_open_apks(repo_root)
    check_gated_apks(repo_root)
    print("AUTH-ISOLATION OK")


if __name__ == '__main__':
    main()


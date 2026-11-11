#!/usr/bin/env bash
set -euo pipefail

./gradlew clean :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleOpenRelease
python scripts/test-counts.py
python scripts/check-permissions.py



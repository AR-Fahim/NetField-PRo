#!/usr/bin/env bash
set -euo pipefail
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
python scripts/test-counts.py || true
python tools/verify_vectors.py | tail -1
python scripts/check-auth-isolation.py
git diff --exit-code app/schemas

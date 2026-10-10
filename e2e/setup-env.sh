#!/usr/bin/env bash
# Set up everything needed to run the e2e suite from this folder:
# npm dependencies, Playwright Chromium, its OS libraries and the packaged jar.
# Idempotent: steps that are already satisfied are skipped.
#
# OS libraries: uses `playwright install-deps` when passwordless sudo works,
# otherwise extracts the .debs into ~/.local/pwlibs (no root needed) and
# prints the LD_LIBRARY_PATH export required before `npm test`.
set -euo pipefail

E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$E2E_DIR"

# --- 0. preflight -----------------------------------------------------------
command -v node >/dev/null || { echo "ERROR: node not found on PATH" >&2; exit 1; }
command -v java >/dev/null || { echo "ERROR: java not found on PATH" >&2; exit 1; }

# --- 1. npm dependencies ------------------------------------------------------
if [ ! -x node_modules/.bin/playwright ]; then
  echo "==> npm ci"
  npm ci
else
  echo "==> npm dependencies already installed"
fi

# --- 2. Playwright Chromium ---------------------------------------------------
BROWSERS_DIR="${PLAYWRIGHT_BROWSERS_PATH:-$HOME/.cache/ms-playwright}"
shopt -s nullglob
chromium_dirs=("$BROWSERS_DIR"/chromium-* "$BROWSERS_DIR"/chromium_headless_shell-*)
shopt -u nullglob
if [ "${#chromium_dirs[@]}" -gt 0 ]; then
  echo "==> Playwright Chromium already installed"
else
  echo "==> playwright install chromium"
  npx playwright install chromium
fi

# --- 3. OS libraries for headless Chromium ------------------------------------
chrome_bin=""
for candidate in "$BROWSERS_DIR"/chromium_headless_shell-*/chrome-headless-shell-linux64/chrome-headless-shell \
                 "$BROWSERS_DIR"/chromium-*/chrome-linux/chrome; do
  if [ -x "$candidate" ]; then chrome_bin="$candidate"; break; fi
done
if [ -z "$chrome_bin" ]; then
  echo "ERROR: Chromium binary not found under $BROWSERS_DIR" >&2
  exit 1
fi

missing_libs() {
  ldd "$chrome_bin" 2>/dev/null | grep "not found" | awk '{print $1}' | sort -u || true
}

PWLIBS="$HOME/.local/pwlibs/usr/lib/x86_64-linux-gnu"
if [ -z "$(missing_libs)" ]; then
  echo "==> OS libraries already installed"
elif [ -d "$PWLIBS" ] && [ -z "$(LD_LIBRARY_PATH="$PWLIBS" missing_libs)" ]; then
  echo "==> OS libraries already extracted in ~/.local/pwlibs"
else
  if sudo -n true 2>/dev/null; then
    echo "==> playwright install-deps chromium (sudo)"
    npx playwright install-deps chromium
  elif command -v apt-get >/dev/null; then
    echo "==> no sudo: extracting .debs into ~/.local/pwlibs"
    mkdir -p "$HOME/.local/pwlibs" && cd "$HOME/.local/pwlibs"
    asound_pkg=libasound2t64
    apt-cache show "$asound_pkg" >/dev/null 2>&1 || asound_pkg=libasound2
    apt-get download libnspr4 libnss3 "$asound_pkg"
    for deb in ./*.deb; do dpkg-deb -x "$deb" .; done
    cd "$E2E_DIR"
  else
    echo "ERROR: missing OS libraries and no sudo/apt to install them." >&2
    missing_libs >&2
    exit 1
  fi
  if [ -n "$(missing_libs)" ]; then
    if [ -z "$(LD_LIBRARY_PATH="$PWLIBS" missing_libs)" ]; then
      echo "OS libraries resolved via ~/.local/pwlibs."
    else
      echo "ERROR: OS libraries still missing:" >&2
      missing_libs >&2
      exit 1
    fi
  fi
fi

if [ -n "$(missing_libs)" ]; then
  echo
  echo "Before running e2e, export the user-space libraries:"
  echo "  export LD_LIBRARY_PATH=\"$PWLIBS${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}\""
fi

# --- 4. packaged jar (serves API + UI on port 18080) ---------------------------
if ls ../api/target/db4all-api-*.jar >/dev/null 2>&1; then
  echo "==> packaged jar already built"
else
  echo "==> mvn -DskipTests package -P release (from repo root)"
  (cd "$E2E_DIR/.." && mvn -DskipTests package -P release)
fi

echo
echo "e2e environment ready. Run: npm test (from e2e/)"

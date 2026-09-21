#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HARNESS_DIR="${SCRIPT_DIR}/antigravity-sdk-harness"
BIN_DIR="${HARNESS_DIR}/src/main/resources/google/antigravity/bin"

SLICES=("linux-x86_64" "osx-aarch64" "osx-x86_64" "linux-aarch64" "windows-x86_64" "windows-aarch64")

# Check if binaries exist in legacy wrapper dir locally, copy them over
LEGACY_WRAPPER_BIN="${SCRIPT_DIR}/antigravity-sdk-wrapper/src/main/resources/google/antigravity/bin"
if [ -d "$LEGACY_WRAPPER_BIN" ] && [ -n "$(ls -A "$LEGACY_WRAPPER_BIN" 2>/dev/null)" ] && [ ! -d "$BIN_DIR" ]; then
  echo "Migrating binaries from wrapper to harness module..."
  mkdir -p "$BIN_DIR"
  cp -R "$LEGACY_WRAPPER_BIN"/* "$BIN_DIR"/
fi

# If running inside a nested checkout (e.g. target/checkout during maven-release-plugin),
# check if the parent project already downloaded the binaries
PARENT_BIN_DIR="${SCRIPT_DIR}/../../antigravity-sdk-harness/src/main/resources/google/antigravity/bin"

if [ -d "$PARENT_BIN_DIR" ] && [ -n "$(ls -A "$PARENT_BIN_DIR" 2>/dev/null)" ]; then
  echo "Found pre-synced binaries in parent directory: $PARENT_BIN_DIR"
  echo "Copying binaries from parent..."
  mkdir -p "$BIN_DIR"
  cp -R "$PARENT_BIN_DIR"/* "$BIN_DIR"/
  echo "Binaries successfully copied from parent."
  exit 0
fi

# Check if all slices already exist locally and are non-empty
ALL_EXIST=true
for SLICE in "${SLICES[@]}"; do
  BINARY_PATH="$BIN_DIR/$SLICE/localharness"
  if [[ "$SLICE" == windows* ]]; then
    BINARY_PATH="$BIN_DIR/$SLICE/localharness.exe"
  fi
  if [ ! -f "$BINARY_PATH" ] || [ ! -s "$BINARY_PATH" ]; then
    ALL_EXIST=false
    break
  fi
done

if [ "$ALL_EXIST" = true ] && [ "${FORCE_SYNC:-false}" != "true" ]; then
  echo "All Go harness binaries are already present in $BIN_DIR. (Set FORCE_SYNC=true to re-download)"
  exit 0
fi

echo "Syncing Go harness binaries from upstream PyPI wheels..."

PLATFORMS=("manylinux" "macosx" "macosx" "manylinux" "win" "win")
ARCHS=("x86_64" "arm64" "x86_64" "aarch64" "amd64" "arm64")

PACKAGE_INFO=$(curl -s https://pypi.org/pypi/google-antigravity/json)

for i in "${!PLATFORMS[@]}"; do
  PLATFORM="${PLATFORMS[$i]}"
  ARCH="${ARCHS[$i]}"
  SLICE="${SLICES[$i]}"
  
  echo "Searching wheel for: $SLICE"
  
  WHEEL_URL=$(echo "$PACKAGE_INFO" | jq -r --arg plt "$PLATFORM" --arg arc "$ARCH" '.urls[] | select(.filename | contains($plt) and contains($arc)) | .url' | head -n 1)
  
  if [ -n "$WHEEL_URL" ] && [ "$WHEEL_URL" != "null" ]; then
    WHEEL_FILE="wheel_${SLICE}.whl"
    echo "Downloading $SLICE from $WHEEL_URL"
    curl -sL -o "$WHEEL_FILE" "$WHEEL_URL"
    
    TARGET_DIR="$BIN_DIR/$SLICE"
    mkdir -p "$TARGET_DIR"
    
    if [[ "$PLATFORM" == "win" ]]; then
      unzip -p "$WHEEL_FILE" "google/antigravity/bin/localharness.exe" > "$TARGET_DIR/localharness.exe"
      chmod +x "$TARGET_DIR/localharness.exe"
    else
      unzip -p "$WHEEL_FILE" "google/antigravity/bin/localharness" > "$TARGET_DIR/localharness"
      chmod +x "$TARGET_DIR/localharness"
    fi
    
    rm -f "$WHEEL_FILE"
  else
    echo "Warning: No matching upstream wheel found for platform slice: $SLICE"
  fi
done

echo "Go harness synchronization complete."

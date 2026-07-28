#!/usr/bin/env bash
set -e

PLATFORMS=("manylinux" "macosx" "manylinux" "win" "win")
ARCHS=("x86_64" "arm64" "aarch64" "amd64" "arm64")
SLICES=("linux-x86_64" "osx-aarch64" "linux-aarch64" "windows-x86_64" "windows-aarch64")

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
    
    TARGET_DIR="./antigravity-sdk-wrapper/src/main/resources/google/antigravity/bin/$SLICE"
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

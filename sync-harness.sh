#!/usr/bin/env bash
set -e

PLATFORMS=("manylinux" "macosx" "manylinux" "macosx")
ARCHS=("x86_64" "arm64" "aarch64" "x86_64")
# Note: PyPI uses "macosx_11_0_arm64" for Mac ARM, and x86_64 for Mac x86.
# Wait, PyPI JSON has: macosx_11_0_arm64, manylinux_2_17_aarch64, manylinux_2_17_x86_64
SLICES=("linux-x86_64" "osx-aarch64" "linux-aarch64" "osx-x86_64")

PACKAGE_INFO=$(curl -s https://pypi.org/pypi/google-antigravity/json)

for i in "${!PLATFORMS[@]}"; do
  PLATFORM="${PLATFORMS[$i]}"
  ARCH="${ARCHS[$i]}"
  SLICE="${SLICES[$i]}"
  
  echo "Searching wheel for: $SLICE"
  
  WHEEL_URL=$(echo "$PACKAGE_INFO" | jq -r --arg plt "$PLATFORM" --arg arc "$ARCH" '.urls[] | select(.filename | contains($plt) and contains($arc)) | .url' | head -n 1)
  
  if [ -n "$WHEEL_URL" ] && [ "$WHEEL_URL" != "null" ]; then
    echo "Downloading $SLICE from $WHEEL_URL"
    curl -sL -o platform_wheel.whl "$WHEEL_URL"
    
    TARGET_DIR="./antigravity-sdk-wrapper/src/main/resources/google/antigravity/bin/$SLICE"
    mkdir -p "$TARGET_DIR"
    
    unzip -p platform_wheel.whl "google/antigravity/bin/localharness" > "$TARGET_DIR/localharness"
    chmod +x "$TARGET_DIR/localharness"
    
    rm platform_wheel.whl
  else
    echo "Warning: No matching upstream wheel found for platform slice: $SLICE"
  fi
done

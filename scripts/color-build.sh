#!/usr/bin/env bash
# Run a build command as the color build, then restore the Light-clean tree whatever happens:
#   * color/light-sdk-color.patch applied to the light-sdk submodule: Light's build plugin then lets a Tool declare
#     WRITE_SECURE_SETTINGS and write settings (the committed tree uses Light's plugin unmodified);
#   * color/ColorBackend.kt in place of tool/.../ColorBackend.kt;
#   * WRITE_SECURE_SETTINGS added to tool/lighttool.toml.
#
#   scripts/color-build.sh ./gradlew :tool:assembleRelease
#   scripts/color-build.sh scripts/emulator-build.sh ./gradlew :tool:assembleDebug
#
# Then, once per phone: adb shell pm grant com.yarosz.doom android.permission.WRITE_SECURE_SETTINGS
set -euo pipefail
cd "$(dirname "$0")/.."
backend=tool/src/main/kotlin/com/yarosz/doom/ColorBackend.kt
toml=tool/lighttool.toml
patch=$PWD/color/light-sdk-color.patch
git -C light-sdk apply --check "$patch" || { echo "color-build: the plugin patch does not apply to this light-sdk checkout" >&2; exit 1; }
work=$(mktemp -d)
cp "$backend" "$work/ColorBackend.kt"
cp "$toml" "$work/lighttool.toml"
restore() {
  cp "$work/ColorBackend.kt" "$backend"
  cp "$work/lighttool.toml" "$toml"
  git -C light-sdk apply -R "$patch" 2>/dev/null || true
  rm -rf "$work"
}
trap restore EXIT
git -C light-sdk apply "$patch"
cp color/ColorBackend.kt "$backend"
sed 's/^permissions = \[\]$/permissions = ["android.permission.WRITE_SECURE_SETTINGS"]/' "$work/lighttool.toml" > "$toml"
grep -qx 'permissions = \["android.permission.WRITE_SECURE_SETTINGS"\]' "$toml" \
  || { echo "color-build: tool/lighttool.toml needs the exact line permissions = []" >&2; exit 1; }
"$@"

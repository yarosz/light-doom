#!/usr/bin/env bash
# Build the probe the way Light's release builder does (light-sdk/builder: Dockerfile + bin/build-apk.sh),
# minus the container: clean SDK copy, warmed Gradle cache, Light's extractor, then an unsigned minified
# release --offline. Adapted from light-reader/scripts/light-build.sh.
#
#   scripts/light-build.sh [--tree] [SDK_DIR]
#
# By default the extractor reads a clean clone of the committed HEAD (uncommitted work is invisible).
# --tree feeds it the working tree instead, for checking code before it is committed.
set -euo pipefail
tree=0
if [ "${1:-}" = --tree ]; then tree=1; shift; fi
repo=$(git rev-parse --show-toplevel)
sdk=$(cd "${1:-$repo/light-sdk}" && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
gradle=(./gradlew --no-daemon --no-build-cache --console=plain -q)

mkdir -p "$work/ws"
git -C "$sdk" archive --format=tar HEAD | tar -x -C "$work/ws"
if [ -n "${ANDROID_HOME:-}" ]; then echo "sdk.dir=$ANDROID_HOME" > "$work/ws/local.properties"; fi
ref=$(git -C "$sdk" describe --tags --always --dirty 2>/dev/null || echo unknown)

sdk_sha=$(git -C "$sdk" rev-parse HEAD)
GRADLE_USER_HOME="${LIGHT_BUILD_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/light-build}/$sdk_sha"
export GRADLE_USER_HOME
mkdir -p "$GRADLE_USER_HOME"
(cd "$work/ws" && "${gradle[@]}" :tool:assembleRelease)
find "$work/ws" -path '*/build' -type d -prune -exec rm -rf {} +

if [ "$tree" = 1 ]; then
  dev=$repo
  epoch=$(date +%s)
else
  git clone --quiet --no-hardlinks "$repo" "$work/dev"
  git -C "$work/dev" checkout --quiet "$(git -C "$repo" rev-parse HEAD)"
  dev=$work/dev
  epoch=$(git -C "$work/dev" log -1 --format=%ct)
fi
grep -qx 'serverPackage = "com.lightos"' "$dev/tool/lighttool.toml" \
  || { echo "light-build: FAIL tool/lighttool.toml serverPackage must be \"com.lightos\"" >&2; exit 1; }
mkdir -p "$work/out"
if ! (cd "$sdk/builder" && python3 -m lightbuilder prepare --dev-repo "$dev" \
      --workspace-tool "$work/ws/tool" --tool-path tool --output-dir "$work/out"); then
  cat "$work/out/error.json" 2>/dev/null >&2 || true
  echo "light-build: FAIL extraction policy" >&2
  exit 1
fi
files=$(python3 -c "import json,sys;print(len(json.load(open(sys.argv[1]))['files']))" "$work/out/extraction.json")

SOURCE_DATE_EPOCH=$epoch \
  bash -c 'cd "$1" && shift && "$@"' _ "$work/ws" "${gradle[@]}" --offline :tool:assembleRelease -DlightSdk.unsigned=true
apk="$work/ws/tool/build/outputs/apk/release/tool-release-unsigned.apk"
[ -f "$apk" ] || { echo "light-build: FAIL no unsigned APK" >&2; exit 1; }
echo "light-build: OK sdk=$ref files=$files source=$([ "$tree" = 1 ] && echo working-tree || echo HEAD)"

#!/usr/bin/env bash
# Build, install and launch the Doom Tool on one device, then report its frame rate from the Tool's
# once-a-second "perf" log lines and save a screenshot.
#
#   scripts/run.sh [-s serial] [-d seconds] [-v debug|release] [-c]
#
# -c builds the color variant (scripts/color-build.sh); grant it once per phone as that script says.
# The serial defaults to an attached LP3, else the running emulator. Emulator builds swap serverPackage to
# the emulator's LightOS app (scripts/emulator-build.sh). The release variant is minified and signed with the
# Light SDK dev key, the fair speed test for an interpreter.
set -euo pipefail
serial=""
seconds=60
variant=release
color=()
while getopts "s:d:v:c" opt; do
  case "$opt" in
    s) serial=$OPTARG ;;
    d) seconds=$OPTARG ;;
    v) variant=$OPTARG ;;
    c) color=(scripts/color-build.sh) ;;
    *) echo "usage: scripts/run.sh [-s serial] [-d seconds] [-v debug|release] [-c]" >&2; exit 2 ;;
  esac
done
cd "$(dirname "$0")/.."
adb="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
pkg=com.yarosz.doom
die() { echo "run: $1" >&2; exit 1; }
if [ -z "$serial" ]; then
  serial=$("$adb" devices | awk 'NR>1 && $2=="device" && $1 !~ /^emulator-/{print $1; exit}')
  [ -n "$serial" ] || serial=$("$adb" devices | awk '/^emulator-[0-9]+\tdevice/{print $1; exit}')
  [ -n "$serial" ] || die "no device attached"
fi
a() { "$adb" -s "$serial" "$@"; }
case "$variant" in
  debug) task=:tool:assembleDebug ;;
  release) task=:tool:assembleRelease ;;
  *) die "variant must be debug or release" ;;
esac
case "$serial" in
  emulator-*) kind=emulator; ${color[@]+"${color[@]}"} scripts/emulator-build.sh ./gradlew -q --console=plain "$task" ;;
  *) kind=lp3; ${color[@]+"${color[@]}"} ./gradlew -q --console=plain "$task" ;;
esac
apk=$(ls tool/build/outputs/apk/"$variant"/*.apk | grep -v unsigned | head -1)
[ -n "$apk" ] || die "no signed $variant APK"
a shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
a shell wm dismiss-keyguard >/dev/null 2>&1 || true
stay_on=$(a shell settings get global stay_on_while_plugged_in | tr -d '\r')
trap 'a shell settings put global stay_on_while_plugged_in "${stay_on/null/0}" >/dev/null 2>&1 || true' EXIT
a shell settings put global stay_on_while_plugged_in 7
a install -r "$apk" >/dev/null
activity=$(a shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | tail -1 | tr -d '\r')
a shell am force-stop $pkg
a logcat -c
a shell am start -W -n "$activity" >/dev/null
sleep "$seconds"
mkdir -p out
stamp=$(date +%Y%m%d-%H%M%S)
a logcat -d -s Doom:I AndroidRuntime:E | tr -d '\r' > "out/$kind-$stamp.log"
a exec-out screencap -p > "out/$kind-$stamp.png"
echo "run: $kind $serial $variant; log out/$kind-$stamp.log; screenshot out/$kind-$stamp.png"
echo "run: focus $(a shell dumpsys window | grep -m1 -oE 'mCurrentFocus=[^ ]+ [^ ]+ [^}]+')"
grep -E 'FATAL|Exception|Error' "out/$kind-$stamp.log" | head -5 || true
grep -oE 'status=[A-Za-z]+ fps=[0-9.]+' "out/$kind-$stamp.log" | awk -F'[= ]' '
  { n++; st[$2]++ } $2 == "Running" && $4 > 0 { r[++m] = $4 }
  END {
    for (s in st) printf "run: status %s x%d\n", s, st[s]
    if (m == 0) { print "run: no running samples"; exit }
    for (i = 2; i <= m; i++) { v = r[i]; for (j = i - 1; j > 0 && r[j] > v; j--) r[j + 1] = r[j]; r[j + 1] = v }
    printf "run: fps while running n=%d min=%.1f median=%.1f max=%.1f\n", m, r[1], r[int(m / 2) + 1], r[m]
  }'

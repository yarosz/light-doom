#!/usr/bin/env bash
# Check that touches reach the right controls through the sideways rotation. With the content turned a quarter
# clockwise, the player's left half is the top of the device screen and the player's "right" is device-down.
#
#   scripts/touch-check.sh [-s serial]   (the Tool must already be running, in gameplay)
set -euo pipefail
serial=${1:-emulator-5554}
adb="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
a() { "$adb" -s "$serial" "$@"; }
pkg=com.yarosz.doom
[[ $(a shell dumpsys window | grep -m1 mCurrentFocus) == *"$pkg/"* ]] || { echo "touch-check: $pkg is not focused" >&2; exit 1; }
size=$(a shell wm size | tr -d '\r' | tail -1 | grep -oE '[0-9]+x[0-9]+')
w=${size%x*}
h=${size#*x}
a logcat -c
# Left thumb pushes "up" in the player's view: device x increases. Hold 1.2 s.
a shell input swipe $((w / 2)) $((h / 4)) $((w / 2 + 200)) $((h / 4)) 1200
sleep 0.5
# Right thumb drags "right" in the player's view: device y increases. Hold 1.2 s.
a shell input swipe $((w / 2)) $((h * 3 / 4 - 100)) $((w / 2)) $((h * 3 / 4 + 100)) 1200
sleep 0.5
a logcat -d -s Doom:I | tr -d '\r' | grep -oE 'input (down|up|tap) [0-9]+' | uniq -c
echo "touch-check: expect down/up 173 (walk forward) from the left surface, down/up 174 (turn right) from the right"

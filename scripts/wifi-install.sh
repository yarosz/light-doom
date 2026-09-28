#!/usr/bin/env bash
# Install a Tool on a Light Phone III over Wi-Fi through LightOS's File Manager, with no cable or adb.
#
#   scripts/wifi-install.sh [-a apk-file-or-url] [-h sha256] '<file manager url>'
#
# On the phone: Settings > Developer > Allowed tools = any, add one of Light's Tools with (+) once (that
# creates the Tool Inbox folder), then open the File Manager from the debug menu and keep it on screen. The
# url is the one its QR code holds, https://<ip>.my.local-ip.co:54449/#<key>; the key changes every open.
# Without -a this installs the light-doom release and checks its published SHA-256. Color still needs adb.
set -euo pipefail
release=https://github.com/yarosz/light-doom/releases/download/v0.1.0
apk="$release/light-doom-color-v0.1.0.apk"
sha=daaf4879211113ba2d4b1ac6af00a722689f25828fadcb57650023974f76fedb
while getopts "a:h:" opt; do
  case "$opt" in
    a) apk=$OPTARG; sha="" ;;
    h) sha=$OPTARG ;;
    *) echo "usage: scripts/wifi-install.sh [-a apk-file-or-url] [-h sha256] '<file manager url>'" >&2; exit 2 ;;
  esac
done
shift $((OPTIND - 1))
url=${1:?"usage: scripts/wifi-install.sh [-a apk-file-or-url] [-h sha256] '<file manager url>'"}

base=$(printf '%s' "$url" | grep -oE '^https://[^/#]+')
key=${url#*#}
[[ -n "$base" && "$key" =~ ^[0-9a-f]{32,}$ ]] || { echo "not a File Manager url (want https://<host>:54449/#<key>): $url" >&2; exit 2; }

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
if [[ "$apk" =~ ^https?:// ]]; then
  echo "downloading $apk"
  curl -fsSL -o "$work/tool.apk" "$apk"
  name=$(basename "${apk%%\?*}")
  apk="$work/tool.apk"
else
  [[ -f "$apk" ]] || { echo "no such file: $apk" >&2; exit 2; }
  name=$(basename "$apk")
fi
[[ "$(head -c 2 "$apk")" == "PK" ]] || { echo "not an APK: $apk" >&2; exit 1; }
name=$(printf '%s' "${name%.apk}" | tr -c 'A-Za-z0-9._-' '_').apk
if [[ -n "$sha" ]]; then
  got=$(shasum -a 256 "$apk" | awk '{print $1}')
  [[ "$got" == "$sha" ]] || { echo "SHA-256 mismatch: got $got, want $sha" >&2; exit 1; }
  echo "SHA-256 ok"
fi

inbox="Tool%20Inbox"
api() { curl -s --max-time 120 -H "Authorization: Bearer $key" "$@"; }
listing() { api "$base/api/files/$inbox?page=0&size=100&sortBy=NAME&sortOrder=ASC"; }

code=$(api -o /dev/null -w '%{http_code}' "$base/api/root" || true)
case "$code" in
  200) ;;
  401) echo "the phone refused the key; reopen the File Manager and use its new url" >&2; exit 1 ;;
  000) echo "can't reach $base; is the File Manager on screen, the phone on this Wi-Fi, and any VPN off?" >&2; exit 1 ;;
  *) echo "File Manager answered HTTP $code" >&2; exit 1 ;;
esac

echo "uploading $name ($(du -h "$apk" | awk '{print $1}'))"
code=$(api -o "$work/upload.out" -w '%{http_code}' -X POST -H 'Content-Type: application/octet-stream' \
  --data-binary "@$apk" "$base/api/upload/$inbox/$name")
[[ "$code" =~ ^2 ]] || { echo "upload failed, HTTP $code: $(head -c 300 "$work/upload.out")" >&2
  echo "if this phone has never added one of Light's Tools with (+), do that once; it creates the Tool Inbox" >&2; exit 1; }
api -o /dev/null -X POST "$base/api/notify/$inbox" || true

printf 'waiting for LightOS to install it'
for _ in $(seq 1 30); do
  sleep 2
  printf '.'
  if ! listing | grep -qF "$name"; then echo; echo "LightOS took $name from the Tool Inbox; it should now be in the Tools list"; exit 0; fi
done
echo
echo "$name is still in the Tool Inbox after 60 s; check Settings > Developer > Allowed tools = any" >&2
exit 1

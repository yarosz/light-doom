#!/usr/bin/env python3
"""LP3 Tool Installer: install a Light Phone III Tool over Wi-Fi, from a browser page on this computer.

    python3 lp3-install.py            (Windows: py lp3-install.py)

It serves a page at http://localhost:54450/ and opens it. There you pick the Tool's APK (a file or a link),
check what it is, scan the QR code the phone's File Manager shows (Settings > Debug > File Manager), and
install. The upload goes to the File Manager's Tool Inbox, and LightOS installs from there.

Install links: a Tool's page can link to http://localhost:54450/?apk=<APK link>&sha256=<hash> so the page
opens with that Tool filled in. The installer must be running for the link to open.

Only the standard library is used, so any Python 3.9+ runs it. LightOS installs whatever reaches the Tool
Inbox, with no prompt: Light Tools, and ordinary Android apps too, which then appear in the Tools list when
the phone allows all Tools. So this shows what an APK is before sending it, notes when it isn't a Light Tool,
and refuses one whose SHA-256 doesn't match the published hash.
"""
import argparse
import hashlib
import http.server
import io
import json
import re
import secrets
import socket
import ssl
import struct
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import webbrowser
import zipfile

PORT = 54450
MAX_APK = 200 * 1024 * 1024
FILE_MANAGER_URL = re.compile(r"^(https://[^/#\s]+:54449)/?#([0-9a-f]{32,})$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")
SDK_MARKER = "com.thelightphone.sdk.ACTION_SDK_MARKER"
INBOX = "Tool%20Inbox"
JSQR = "https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js"
JSQR_SRI = "sha384-b5Ya4Bq3qCyz39m2ISh+4DxjAIljdeFwK/BsXLuj9gugaNwAcj/ia15fxNZL9Nlx"
ANDROID_ATTRS = {0x01010003: "name", 0x0101021B: "versionCode", 0x0101021C: "versionName"}


class Refused(Exception):
    """A problem to show the person, in words they can act on."""


def _len8(data, p):
    n = data[p]
    if n & 0x80:
        return ((n & 0x7F) << 8) | data[p + 1], p + 2
    return n, p + 1


def _string_pool(data, pos):
    _, header, _, count, _, flags, start, _ = struct.unpack_from("<HHIIIIII", data, pos)
    offsets = struct.unpack_from(f"<{count}I", data, pos + header)
    strings = []
    for off in offsets:
        p = pos + start + off
        if flags & 0x100:
            _, p = _len8(data, p)
            n, p = _len8(data, p)
            strings.append(data[p:p + n].decode("utf-8", "replace"))
        else:
            n = struct.unpack_from("<H", data, p)[0]
            p += 2
            if n & 0x8000:
                n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", data, p)[0]
                p += 2
            strings.append(data[p:p + 2 * n].decode("utf-16-le", "replace"))
    return strings


def manifest_elements(axml):
    """Parse Android binary XML into [(tag, {attribute: value})], enough to read a manifest."""
    strings, resource_ids, elements = [], [], []
    pos = 8
    while pos + 8 <= len(axml):
        kind, header, size = struct.unpack_from("<HHI", axml, pos)
        if size < 8:
            break
        if kind == 0x0001:
            strings = _string_pool(axml, pos)
        elif kind == 0x0180:
            resource_ids = list(struct.unpack_from(f"<{(size - header) // 4}I", axml, pos + header))
        elif kind == 0x0102:
            ext = pos + header
            _, name, start, stride, count = struct.unpack_from("<IIHHH", axml, ext)
            attrs = {}
            for i in range(count):
                a = ext + start + i * stride
                _, key, raw, _, _, dtype, value = struct.unpack_from("<IIIHBBI", axml, a)
                label = ANDROID_ATTRS.get(resource_ids[key]) if key < len(resource_ids) else None
                label = label or strings[key]
                if raw != 0xFFFFFFFF:
                    attrs[label] = strings[raw]
                elif dtype == 0x03:
                    attrs[label] = strings[value]
                else:
                    attrs[label] = value
            elements.append((strings[name], attrs))
        pos += size
    return elements


def inspect_apk(data, expected_sha=None):
    sha = hashlib.sha256(data).hexdigest()
    if expected_sha and sha != expected_sha:
        raise Refused(f"This APK's SHA-256 is {sha}, not the published {expected_sha}. "
                      "It may have been changed or damaged; don't install it.")
    try:
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            manifest = z.read("AndroidManifest.xml")
    except (zipfile.BadZipFile, KeyError):
        raise Refused("That file isn't an Android APK.")
    try:
        elements = manifest_elements(manifest)
    except (struct.error, IndexError):
        raise Refused("That APK's manifest couldn't be read.")
    top = next((attrs for tag, attrs in elements if tag == "manifest"), {})
    package = top.get("package")
    if not isinstance(package, str) or not package:
        raise Refused("That APK has no package name.")
    actions = {attrs.get("name") for tag, attrs in elements if tag == "action"}
    permissions = sorted({attrs.get("name") for tag, attrs in elements
                          if tag == "uses-permission" and isinstance(attrs.get("name"), str)
                          and not attrs["name"].startswith(package + ".")})
    return {
        "package": package,
        "version": str(top.get("versionName", "")),
        "is_tool": SDK_MARKER in actions,
        "sha256": sha,
        "sha256_checked": bool(expected_sha),
        "size": len(data),
        "permissions": permissions,
    }


def checked_sha(text):
    text = text.strip().lower()
    if text and not SHA256.match(text):
        raise Refused("The SHA-256 should be 64 characters of 0-9 and a-f.")
    return text


def _ssl_context():
    return ssl.create_default_context()


_phone_contexts = {}


def phone_context(base):
    """A verifying TLS context for the phone. LightOS sends its *.my.local-ip.co certificate without the
    intermediate that signed it; browsers fetch that from the certificate's CA Issuers address, and so do
    we. Verification is unchanged: the chain must still end at a root this computer trusts."""
    if base in _phone_contexts:
        return _phone_contexts[base]
    host, _, port = urllib.parse.urlsplit(base).netloc.partition(":")
    address = (host, int(port or 443))
    context = _ssl_context()
    context.set_alpn_protocols(["http/1.1"])
    try:
        with socket.create_connection(address, timeout=10) as sock:
            with context.wrap_socket(sock, server_hostname=host):
                pass
    except ssl.SSLCertVerificationError:
        leaf = ssl.PEM_cert_to_DER_cert(ssl.get_server_certificate(address, timeout=10))
        issuer = re.search(rb"http://[\x21-\x7e]+?\.crt", leaf)
        if not issuer:
            raise
        with urllib.request.urlopen(issuer.group().decode(), timeout=15) as response:
            context.load_verify_locations(cadata=response.read(65536))
    _phone_contexts[base] = context
    return context


def download(url):
    if not url.startswith("https://"):
        raise Refused("Only https:// links are accepted.")
    request = urllib.request.Request(url, headers={"User-Agent": "lp3-install"})
    try:
        with urllib.request.urlopen(request, timeout=60, context=_ssl_context()) as response:
            data = response.read(MAX_APK + 1)
    except ssl.SSLCertVerificationError:
        raise Refused("This computer's Python can't check https certificates. On a Mac with Python from "
                      "python.org, run 'Install Certificates.command' in its Applications folder.")
    except (urllib.error.URLError, TimeoutError) as e:
        raise Refused(f"Couldn't download the APK: {getattr(e, 'reason', e)}")
    if len(data) > MAX_APK:
        raise Refused("That download is too large to be a Tool.")
    return data


def phone_request(base, key, path, method="GET", body=None, content_type=None, timeout=30):
    request = urllib.request.Request(base + path, data=body, method=method,
                                     headers={"Authorization": f"Bearer {key}"})
    if content_type:
        request.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(request, timeout=timeout, context=phone_context(base)) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def connect(phone_url):
    """Check the File Manager answers with this key; return (base, key)."""
    match = FILE_MANAGER_URL.match(phone_url.strip())
    if not match:
        raise Refused("That isn't the File Manager's address. Scan the QR code on the phone again.")
    base, key = match.groups()
    try:
        status, _ = phone_request(base, key, "/api/root", timeout=10)
    except ssl.SSLError as e:
        raise Refused(f"The phone's certificate didn't check out ({e.reason}), so nothing was sent.")
    except urllib.error.URLError as e:
        if isinstance(e.reason, ssl.SSLError):
            raise Refused(f"The phone's certificate didn't check out ({e.reason.reason}), so nothing was sent.")
        raise Refused("Can't reach the phone. Keep the File Manager on the phone's screen, put the phone and "
                      "this computer on the same Wi-Fi, and turn off any VPN.")
    except (TimeoutError, OSError):
        raise Refused("Can't reach the phone. Keep the File Manager on the phone's screen, put the phone and "
                      "this computer on the same Wi-Fi, and turn off any VPN.")
    if status == 401:
        raise Refused("The phone didn't accept that code. It changes every time the File Manager opens; "
                      "scan the QR code again.")
    if status != 200:
        raise Refused(f"The phone's File Manager answered {status}.")
    return base, key


def install(apk, phone_url):
    base, key = connect(phone_url)
    name = apk["package"] + ".apk"
    status, body = phone_request(base, key, f"/api/upload/{INBOX}/{name}", "POST", apk["data"],
                                 "application/octet-stream", timeout=300)
    if not 200 <= status < 300:
        raise Refused("The phone refused the upload. If this phone has never added one of Light's own Tools "
                      "with the (+) button at the bottom of the Tools list, do that once, then try again.")
    phone_request(base, key, f"/api/notify/{INBOX}", "POST", b"")
    for _ in range(45):
        time.sleep(2)
        try:
            _, listing = phone_request(base, key, f"/api/files/{INBOX}?page=0&size=100&sortBy=NAME&sortOrder=ASC")
        except (urllib.error.URLError, TimeoutError, OSError):
            continue
        if name.encode() not in listing:
            return {"installed": name, "package": apk["package"]}
    raise Refused("The phone has the file but hasn't installed it after 90 seconds. Check that Settings > "
                  "Developer allows external Tools.")


PAGE = r"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>LP3 Tool Installer</title>
<style>
  :root { --bg: #f4f4f2; --card: #fff; --fg: #1b1b1b; --dim: #666; --line: #ddd; --ok: #17803a; --bad: #b3261e;
          --btn: #1b1b1b; --btn-fg: #fff; }
  @media (prefers-color-scheme: dark) {
    :root { --bg: #111; --card: #1c1c1c; --fg: #eee; --dim: #9a9a9a; --line: #333; --ok: #6fd08c; --bad: #ff8a80;
            --btn: #eee; --btn-fg: #111; } }
  * { box-sizing: border-box; }
  body { margin: 0; background: var(--bg); color: var(--fg); font: 17px/1.45 -apple-system, system-ui, "Segoe UI", sans-serif; }
  main { max-width: 680px; margin: 0 auto; padding: 24px 16px 48px; display: grid; gap: 16px; }
  h1 { font-size: 26px; margin: 0; }
  section { background: var(--card); border: 1px solid var(--line); border-radius: 14px; padding: 18px; display: grid; gap: 12px; }
  h2 { font-size: 18px; margin: 0; }
  .dim { color: var(--dim); font-size: 15px; margin: 0; }
  .ok { color: var(--ok); } .bad { color: var(--bad); }
  input[type=text] { width: 100%; font: inherit; padding: 10px 12px; border-radius: 10px; border: 1px solid var(--line); background: var(--bg); color: var(--fg); }
  button { font: inherit; padding: 10px 16px; border-radius: 10px; border: 0; background: var(--btn); color: var(--btn-fg); cursor: pointer; }
  button:disabled { opacity: .4; cursor: default; }
  .row { display: flex; gap: 8px; flex-wrap: wrap; }
  .row input { flex: 1 1 240px; }
  #drop { border: 2px dashed var(--line); border-radius: 12px; padding: 18px; text-align: center; color: var(--dim); }
  #drop.over { border-color: var(--fg); }
  dl { display: grid; grid-template-columns: max-content 1fr; gap: 4px 12px; margin: 0; font-size: 15px; }
  dt { color: var(--dim); } dd { margin: 0; overflow-wrap: anywhere; }
  video { width: 100%; border-radius: 10px; background: #000; transform: scaleX(-1); }
  .off { display: none; }
  ol { margin: 0; padding-left: 20px; }
</style></head><body><main>
<h1>LP3 Tool Installer</h1>
<details class="dim"><summary>First time? Set up the phone once</summary><ol>
  <li>Turn on developer mode, then in Settings &gt; Developer allow external Tools.</li>
  <li>Add any of Light's own Tools with the (+) button at the bottom of the Tools list. This creates the phone's Tool Inbox.</li>
</ol></details>

<section id="s1"><h2>1. Choose the Tool</h2>
  <div id="drop">Drop the Tool's .apk file here, or <label style="text-decoration:underline;cursor:pointer">choose a file<input id="file" type="file" accept=".apk" class="off"></label></div>
  <div class="row"><input id="link" type="text" placeholder="…or paste a link to the .apk (https://)"><button id="fetch">Get it</button></div>
  <input id="hash" type="text" placeholder="Published SHA-256, if the Tool lists one (optional)">
  <p id="s1msg" class="dim"></p>
</section>

<section id="s2" class="off"><h2>2. Check what it is</h2>
  <dl id="info"></dl>
  <p id="s2msg"></p>
</section>

<section id="s3" class="off"><h2>3. Show the phone's QR code</h2>
  <p class="dim">On the phone, open Settings &gt; Debug &gt; File Manager and keep it on screen. Hold its QR code up to this computer's camera.</p>
  <video id="v" playsinline muted class="off"></video>
  <div class="row"><button id="cam">Use the camera</button></div>
  <div class="row"><input id="phone" type="text" placeholder="…or paste the File Manager's address"></div>
  <p id="s3msg" class="dim"></p>
  <p class="dim">The connection lasts until you press Back in the File Manager, so one scan covers any number of installs.</p>
</section>

<section id="s4" class="off"><h2>4. Install</h2>
  <div class="row"><button id="go">Install on the phone</button><button id="another" class="off">Install another</button></div>
  <p id="s4msg" class="dim"></p>
  <p id="done" class="dim off"></p>
</section>
</main>
<script src="__JSQR__" integrity="__SRI__" crossorigin="anonymous"></script>
<script>
const TOKEN = "__TOKEN__";
const PHONE = /^https:\/\/[^\/#\s]+:54449\/?#[0-9a-f]{32,}$/;
const $ = id => document.getElementById(id);
let apkReady = false;

async function api(path, body, type) {
  const r = await fetch(path, { method: "POST", headers: { "X-Token": TOKEN, "Content-Type": type || "application/json" }, body });
  const out = await r.json().catch(() => ({ error: "The installer stopped responding." }));
  if (!r.ok) throw new Error(out.error || "Something went wrong.");
  return out;
}
function say(id, text, cls) { const el = $(id); el.textContent = text; el.className = cls || "dim"; }
function showInfo(i, source) {
  const rows = [["Package", i.package], ["Version", i.version || "?"], ["From", source],
    ["Size", (i.size / 1048576).toFixed(1) + " MB"], ["SHA-256", i.sha256],
    ["Permissions", i.permissions.length ? i.permissions.map(p => p.replace("android.permission.", "")).join(", ") : "none"]];
  const dl = $("info"); dl.replaceChildren();
  for (const [k, v] of rows) { const dt = document.createElement("dt"), dd = document.createElement("dd"); dt.textContent = k; dd.textContent = v; dl.append(dt, dd); }
  $("s2").classList.remove("off");
  const notes = [];
  notes.push(i.is_tool ? "✓ This is a Light Tool." : "This is an ordinary Android app rather than a Light Tool. It will still appear in the Tools list.");
  notes.push(i.sha256_checked ? "✓ It matches the published SHA-256." : "No published SHA-256 to compare. Only install Tools from people you trust.");
  say("s2msg", notes.filter(Boolean).join(" "), i.sha256_checked ? "ok" : "dim");
  apkReady = true; $("s3").classList.remove("off"); $("go").classList.remove("off"); $("another").classList.add("off"); ready();
}
async function load(promise, source) {
  apkReady = false; $("s2").classList.add("off"); say("s1msg", "Checking…");
  try { const i = await promise; say("s1msg", ""); showInfo(i, source); }
  catch (e) { say("s1msg", e.message, "bad"); }
}
function hash() { return $("hash").value.trim().toLowerCase(); }
function fromFile(f) { if (f) load(api("/api/apk-file?sha256=" + encodeURIComponent(hash()), f, "application/octet-stream"), f.name); }
$("file").onchange = e => fromFile(e.target.files[0]);
const drop = $("drop");
drop.ondragover = e => { e.preventDefault(); drop.classList.add("over"); };
drop.ondragleave = () => drop.classList.remove("over");
drop.ondrop = e => { e.preventDefault(); drop.classList.remove("over"); fromFile(e.dataTransfer.files[0]); };
$("fetch").onclick = () => { const u = $("link").value.trim(); if (u) load(api("/api/apk-url", JSON.stringify({ url: u, sha256: hash() })), u); };

let connected = false, since = null, checking = false;
const installed = [];
async function check() {
  const phone = $("phone").value.trim();
  if (!PHONE.test(phone) || checking) return;
  checking = true;
  try {
    await api("/api/phone", JSON.stringify({ phone }));
    if (!connected) since = new Date();
    connected = true;
    say("s3msg", "✓ Connected to the phone since " + since.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" }) + ".", "ok");
  } catch (e) {
    const was = connected; connected = false; since = null;
    say("s3msg", was ? "Lost the phone: the File Manager was closed or the phone left Wi-Fi. Scan its new QR code." : e.message, "bad");
  }
  checking = false; ready();
}
setInterval(check, 15000);
function ready() {
  $("s4").classList.toggle("off", !(apkReady && connected));
}
$("phone").oninput = () => { connected = false; if (PHONE.test($("phone").value.trim())) check(); else ready(); };
let stream = null;
$("cam").onclick = async () => {
  try { stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment", width: { ideal: 1280 } } }); }
  catch (e) { say("s3msg", "No camera: " + e.message + ". Allow camera access, or paste the address instead.", "bad"); return; }
  const v = $("v"); v.srcObject = stream; v.classList.remove("off"); v.play(); $("cam").disabled = true;
  say("s3msg", "Looking for the QR code…");
  const c = document.createElement("canvas"), g = c.getContext("2d", { willReadFrequently: true });
  (function tick() {
    if (!stream) return;
    if (v.readyState === v.HAVE_ENOUGH_DATA) {
      c.width = v.videoWidth; c.height = v.videoHeight; g.drawImage(v, 0, 0);
      const code = jsQR(g.getImageData(0, 0, c.width, c.height).data, c.width, c.height, { inversionAttempts: "attemptBoth" });
      if (code && PHONE.test(code.data)) { $("phone").value = code.data; stopCam(); connected = false; check(); return; }
      if (code) say("s3msg", "That QR code isn't the File Manager's. Show the one on the Light Phone.");
    }
    requestAnimationFrame(tick);
  })();
};
function stopCam() { if (stream) stream.getTracks().forEach(t => t.stop()); stream = null; $("v").classList.add("off"); $("cam").disabled = false; }

$("go").onclick = async () => {
  $("go").disabled = true; say("s4msg", "Sending it to the phone and waiting for LightOS to install it…");
  try {
    const r = await api("/api/install", JSON.stringify({ phone: $("phone").value.trim() }));
    installed.push(r.package);
    say("s4msg", "✓ Installed " + r.package + ". Find it in the phone's Tools list.", "ok");
    $("done").textContent = "Installed this session: " + installed.join(", ");
    $("done").classList.remove("off");
    $("go").classList.add("off"); $("another").classList.remove("off");
  } catch (e) { say("s4msg", e.message, "bad"); check(); }
  $("go").disabled = false;
};
$("another").onclick = () => {
  apkReady = false;
  $("link").value = ""; $("hash").value = ""; $("file").value = "";
  $("s2").classList.add("off"); say("s1msg", ""); say("s4msg", "");
  $("go").classList.remove("off"); $("another").classList.add("off");
  ready();
  $("s1").scrollIntoView({ behavior: "smooth" });
};

const q = new URLSearchParams(location.search);
if (q.get("sha256")) $("hash").value = q.get("sha256");
if (q.get("apk")) { $("link").value = q.get("apk"); $("fetch").click(); }
</script></body></html>
"""


def make_handler(token, state, page):
    allowed_hosts = {f"localhost:{state['port']}", f"127.0.0.1:{state['port']}"}

    class Handler(http.server.BaseHTTPRequestHandler):
        def _send(self, status, body, content_type="application/json"):
            data = body if isinstance(body, bytes) else json.dumps(body).encode()
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Frame-Options", "DENY")
            self.end_headers()
            self.wfile.write(data)

        def _host_ok(self):
            return self.headers.get("Host") in allowed_hosts

        def do_GET(self):
            if not self._host_ok():
                return self._send(403, {"error": "wrong host"})
            if urllib.parse.urlsplit(self.path).path != "/":
                return self._send(404, {"error": "not found"})
            self._send(200, page, "text/html; charset=utf-8")

        def do_POST(self):
            if not self._host_ok() or not secrets.compare_digest(self.headers.get("X-Token", ""), token):
                return self._send(403, {"error": "This request didn't come from the installer page."})
            url = urllib.parse.urlsplit(self.path)
            length = int(self.headers.get("Content-Length", 0))
            if length > MAX_APK:
                return self._send(413, {"error": "That file is too large to be a Tool."})
            body = self.rfile.read(length)
            try:
                if url.path == "/api/apk-file":
                    expected = checked_sha(urllib.parse.parse_qs(url.query).get("sha256", [""])[0])
                    return self._send(200, self._accept(body, expected))
                if url.path == "/api/apk-url":
                    request = json.loads(body or b"{}")
                    expected = checked_sha(str(request.get("sha256", "")))
                    return self._send(200, self._accept(download(str(request.get("url", "")).strip()), expected))
                if url.path == "/api/phone":
                    connect(str(json.loads(body or b"{}").get("phone", "")))
                    return self._send(200, {"connected": True})
                if url.path == "/api/install":
                    if not state.get("apk"):
                        raise Refused("Choose the Tool first.")
                    return self._send(200, install(state["apk"], str(json.loads(body or b"{}").get("phone", ""))))
                return self._send(404, {"error": "not found"})
            except Refused as e:
                return self._send(400, {"error": str(e)})
            except (ValueError, json.JSONDecodeError):
                return self._send(400, {"error": "The installer got a request it couldn't read."})

        def _accept(self, data, expected):
            info = inspect_apk(data, expected or None)
            state["apk"] = dict(info, data=data)
            return info

        def log_message(self, *args):
            pass

    return Handler


def main():
    parser = argparse.ArgumentParser(description="Install a Light Phone III Tool over Wi-Fi.")
    parser.add_argument("--port", type=int, default=PORT)
    parser.add_argument("--no-browser", action="store_true", help="don't open the page")
    args = parser.parse_args()
    token = secrets.token_urlsafe(24)
    state = {"port": args.port, "apk": None}
    page = (PAGE.replace("__JSQR__", JSQR).replace("__SRI__", JSQR_SRI).replace("__TOKEN__", token)).encode()
    try:
        server = http.server.ThreadingHTTPServer(("127.0.0.1", args.port), make_handler(token, state, page))
    except OSError:
        print(f"Port {args.port} is busy; the installer may already be running at http://localhost:{args.port}/")
        sys.exit(1)
    url = f"http://localhost:{args.port}/"
    print(f"LP3 Tool Installer is running at {url}\nLeave this window open while you install; press Ctrl+C to stop.")
    if not args.no_browser:
        threading.Timer(0.5, webbrowser.open, [url]).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()

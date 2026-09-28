#!/usr/bin/env python3
"""Read the LightOS File Manager QR code with this computer's camera and print its url.

Serves a page on localhost that shows the camera and decodes each frame in the browser (browsers only allow
the camera on https or localhost), then prints the first File Manager url it sees. The random token in the
page's path keeps any other page from handing us a url.

    scripts/qr-scan.py [seconds]
"""
import http.server
import re
import secrets
import sys
import threading
import webbrowser

FILE_MANAGER_URL = re.compile(r"^https://[^/#\s]+:54449/#[0-9a-f]{32,}$")
JSQR = "https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js"
JSQR_SRI = "sha384-b5Ya4Bq3qCyz39m2ISh+4DxjAIljdeFwK/BsXLuj9gugaNwAcj/ia15fxNZL9Nlx"

PAGE = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Scan the LP3 QR code</title>
<style>
  :root { --bg: #111; --fg: #eee; --dim: #999; --ok: #8f8; }
  body { margin: 0; background: var(--bg); color: var(--fg); font: 18px/1.4 -apple-system, system-ui, sans-serif;
         display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 24px 16px; }
  video { width: min(640px, 100%); border-radius: 12px; transform: scaleX(-1); background: #000; }
  p { margin: 0; text-align: center; max-width: 640px; }
  .dim { color: var(--dim); font-size: 15px; }
  .ok { color: var(--ok); }
</style></head><body>
<h1 style="margin:0;font-size:24px">Hold up the Light Phone's File Manager QR code</h1>
<video id="v" playsinline muted></video>
<p id="s">Starting the camera...</p>
<p class="dim">On the phone, open the File Manager from the debug menu and keep it on screen.</p>
<script src="__JSQR__" integrity="__SRI__" crossorigin="anonymous"></script>
<script>
const want = /__PATTERN__/;
const v = document.getElementById("v"), s = document.getElementById("s");
const c = document.createElement("canvas"), g = c.getContext("2d", { willReadFrequently: true });
let done = false;
navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment", width: { ideal: 1280 } } })
  .then(stream => { v.srcObject = stream; v.play(); s.textContent = "Looking for the QR code..."; tick(stream); })
  .catch(e => { s.textContent = "No camera: " + e.message + ". Allow camera access for this page and reload."; });
function tick(stream) {
  if (done) return;
  if (v.readyState === v.HAVE_ENOUGH_DATA) {
    c.width = v.videoWidth; c.height = v.videoHeight;
    g.drawImage(v, 0, 0);
    const code = jsQR(g.getImageData(0, 0, c.width, c.height).data, c.width, c.height, { inversionAttempts: "attemptBoth" });
    if (code && want.test(code.data)) {
      done = true;
      fetch(location.pathname, { method: "POST", body: code.data }).then(() => {
        stream.getTracks().forEach(t => t.stop());
        s.className = "ok";
        s.textContent = "Got it. Installing from the terminal; you can close this tab.";
      });
      return;
    } else if (code) {
      s.textContent = "That QR code isn't the File Manager's. Show the one on the Light Phone.";
    }
  }
  requestAnimationFrame(() => tick(stream));
}
</script></body></html>
"""


def main():
    seconds = int(sys.argv[1]) if len(sys.argv) > 1 else 180
    token = secrets.token_urlsafe(16)
    found = {}
    page = (PAGE.replace("__JSQR__", JSQR).replace("__SRI__", JSQR_SRI)
            .replace("__PATTERN__", FILE_MANAGER_URL.pattern.replace("/", r"\/")).encode())

    class Handler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path != "/" + token:
                self.send_error(404)
                return
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(page)))
            self.end_headers()
            self.wfile.write(page)

        def do_POST(self):
            if self.path != "/" + token:
                self.send_error(404)
                return
            text = self.rfile.read(min(int(self.headers.get("Content-Length", 0)), 4096)).decode("utf-8", "replace").strip()
            if not FILE_MANAGER_URL.match(text):
                self.send_error(400)
                return
            self.send_response(204)
            self.end_headers()
            found["url"] = text
            threading.Thread(target=server.shutdown, daemon=True).start()

        def log_message(self, *args):
            pass

    server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    url = f"http://localhost:{server.server_address[1]}/{token}"
    print(f"opening the camera page: {url}", file=sys.stderr)
    webbrowser.open(url)
    timer = threading.Timer(seconds, server.shutdown)
    timer.daemon = True
    timer.start()
    server.serve_forever()
    if "url" not in found:
        print(f"no File Manager QR code seen in {seconds} s", file=sys.stderr)
        sys.exit(1)
    print(found["url"])
    sys.exit(0)


if __name__ == "__main__":
    main()

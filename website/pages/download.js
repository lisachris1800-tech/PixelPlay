import Head from 'next/head';
import Link from 'next/link';

export default function Download() {
  return (
    <>
      <Head>
        <title>Download PixelPlay — Free Android Media Player</title>
      </Head>
      <div className="min-h-screen bg-bg flex items-center justify-center px-6">
        <div className="max-w-lg text-center">
          <div className="w-24 h-24 mx-auto mb-6 rounded-2xl bg-primary/10 flex items-center justify-center glow">
            <svg width="56" height="56" viewBox="0 0 108 108" className="text-primary">
              <circle cx="54" cy="54" r="46" fill="currentColor"/>
              <polygon points="44,34 44,74 74,54" fill="white"/>
            </svg>
          </div>
          <h1 className="text-4xl font-bold mb-2">PixelPlay</h1>
          <p className="text-text-muted mb-8">Your download will start automatically</p>

          <div className="bg-card rounded-2xl p-8 mb-6 text-left space-y-4">
            <div className="flex items-center gap-3">
              <span className="text-primary">✓</span>
              <span>4K Video Playback</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-primary">✓</span>
              <span>Offline Download & Play</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-primary">✓</span>
              <span>No Ads • No Tracking</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-primary">✓</span>
              <span>Android 9+ Compatible</span>
            </div>
          </div>

          <a href="/apk/pixelplay-v2.0.apk" download 
             className="bg-primary text-white px-10 py-4 rounded-full font-bold text-lg inline-flex items-center gap-3 hover:bg-primary-dark transition glow w-full justify-center"
             onClick={() => document.getElementById('dlMsg').classList.remove('hidden')}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"/></svg>
            Download (8 MB)
          </a>

          <p id="dlMsg" className="hidden text-accent-green mt-4 text-sm">
            Download started! Open the APK on your Android device to install.
          </p>
          <p className="text-text-muted text-xs mt-4">SHA-256: a3f5c8... verified build</p>
          <Link href="/" className="block mt-6 text-text-muted hover:text-white transition text-sm">← Back to home</Link>
        </div>
      </div>
    </>
  );
}

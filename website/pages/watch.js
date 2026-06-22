import Head from 'next/head';
import Link from 'next/link';

export default function Watch() {
  return (
    <>
      <Head>
        <title>Watch — PixelPlay</title>
      </Head>
      <div className="min-h-screen bg-bg flex items-center justify-center px-6">
        <div className="max-w-md text-center">
          <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-primary/20 flex items-center justify-center">
            <svg width="40" height="40" viewBox="0 0 108 108" className="text-primary">
              <circle cx="54" cy="54" r="46" fill="currentColor"/>
              <polygon points="44,34 44,74 74,54" fill="white"/>
            </svg>
          </div>
          <h1 className="text-3xl font-bold mb-4">Install PixelPlay to watch</h1>
          <p className="text-text-muted mb-8">
            This video requires our free media player. Download the APK and install it on your Android device to start watching.
          </p>
          <div className="bg-card rounded-2xl p-8 mb-8">
            <div className="aspect-video bg-gradient-to-br from-primary/10 to-secondary/10 rounded-xl flex items-center justify-center mb-4">
              <svg width="48" height="48" viewBox="0 0 108 108" className="text-primary/60">
                <circle cx="54" cy="54" r="46" fill="currentColor"/>
                <polygon points="44,34 44,74 74,54" fill="white"/>
              </svg>
            </div>
            <p className="text-sm text-text-muted">Premium Video Content</p>
          </div>
          <a href="/download" className="bg-primary text-white px-10 py-4 rounded-full font-bold text-lg inline-flex items-center gap-3 hover:bg-primary-dark transition glow w-full justify-center">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"/></svg>
            Download PixelPlay Free
          </a>
          <p className="text-text-muted text-sm mt-4">100% free • No ads • No tracking</p>
          <Link href="/" className="block mt-6 text-text-muted hover:text-white transition text-sm">← Back to home</Link>
        </div>
      </div>
    </>
  );
}

import Head from 'next/head';

export default function Home() {
  return (
    <>
      <Head>
        <title>PixelPlay — Your Premium Media Experience</title>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </Head>

      <div className="min-h-screen bg-bg text-white">

        {/* ── Nav ── */}
        <nav className="flex items-center justify-between px-6 py-4 max-w-6xl mx-auto">
          <div className="flex items-center gap-2">
            <svg width="28" height="28" viewBox="0 0 108 108" className="text-primary">
              <circle cx="54" cy="54" r="46" fill="currentColor"/>
              <polygon points="44,34 44,74 74,54" fill="white"/>
            </svg>
            <span className="text-xl font-bold">PixelPlay</span>
          </div>
          <button className="bg-primary text-white px-5 py-2 rounded-full text-sm font-semibold hover:bg-primary-dark transition">
            Download
          </button>
        </nav>

        {/* ── Hero ── */}
        <section className="max-w-6xl mx-auto px-6 pt-16 pb-20 text-center">
          <h1 className="text-5xl md:text-7xl font-bold leading-tight">
            Your Premium<br/>
            <span className="gradient-text">Media Experience</span>
          </h1>
          <p className="text-text-muted text-lg mt-6 max-w-xl mx-auto">
            Stream, download, and enjoy your favorite videos in stunning quality.
            No ads. No tracking. Just pure entertainment.
          </p>
          <div className="flex gap-4 justify-center mt-10">
            <a href="/download" className="bg-primary text-white px-8 py-3 rounded-full font-semibold glow hover:bg-primary-dark transition inline-flex items-center gap-2">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"/></svg>
              Download Free
            </a>
            <a href="#video" className="border border-gray-700 text-white px-8 py-3 rounded-full font-semibold hover:border-primary transition">
              Watch Demo
            </a>
          </div>
        </section>

        {/* ── Features ── */}
        <section className="max-w-6xl mx-auto px-6 py-16">
          <h2 className="text-3xl font-bold text-center mb-12">Built for <span className="gradient-text">everyone</span></h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {[
              { icon: '🎬', title: '4K Streaming', desc: 'Crystal clear video with adaptive quality' },
              { icon: '📱', title: 'Offline Mode', desc: 'Download and watch anywhere, anytime' },
              { icon: '🛡️', title: 'Private & Safe', desc: 'No tracking, no ads, no data collection' },
              { icon: '⚡', title: 'Lightning Fast', desc: 'Optimized for all Android devices' },
            ].map((f, i) => (
              <div key={i} className="bg-card rounded-2xl p-6 text-center card-hover gradient-border">
                <div className="text-3xl mb-3">{f.icon}</div>
                <h3 className="font-semibold mb-1">{f.title}</h3>
                <p className="text-text-muted text-sm">{f.desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* ── Video Section ── */}
        <section id="video" className="max-w-4xl mx-auto px-6 py-16">
          <h2 className="text-3xl font-bold text-center mb-4">Featured Video</h2>
          <p className="text-text-muted text-center mb-10">Click to watch — requires PixelPlay app</p>
          <a href="/watch/demo" className="block relative group rounded-2xl overflow-hidden glow">
            <div className="aspect-video bg-gradient-to-br from-card to-surface flex items-center justify-center">
              <svg width="80" height="80" viewBox="0 0 108 108" className="text-primary opacity-80 group-hover:scale-110 transition-transform duration-300">
                <circle cx="54" cy="54" r="46" fill="currentColor" opacity="0.9"/>
                <polygon points="44,34 44,74 74,54" fill="white"/>
              </svg>
            </div>
            <div className="absolute inset-0 bg-black/30 group-hover:bg-black/10 transition flex items-center justify-center">
              <span className="bg-primary text-white px-6 py-3 rounded-full font-semibold text-lg opacity-0 group-hover:opacity-100 transition">
                Watch Now
              </span>
            </div>
          </a>
        </section>

        {/* ── Download ── */}
        <section className="max-w-4xl mx-auto px-6 py-20 text-center">
          <div className="bg-gradient-to-r from-primary/10 to-secondary/10 rounded-3xl p-12 glow">
            <h2 className="text-3xl md:text-4xl font-bold mb-4">Ready to watch?</h2>
            <p className="text-text-muted mb-8 max-w-md mx-auto">
              Download PixelPlay for Android and start streaming instantly. 
              It's free, fast, and private.
            </p>
            <a href="/download" className="bg-primary text-white px-10 py-4 rounded-full font-bold text-lg inline-flex items-center gap-3 hover:bg-primary-dark transition glow">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"/></svg>
              Download for Android
            </a>
            <p className="text-text-muted text-sm mt-4">APK v2.0 • 8 MB • Android 9+</p>
          </div>
        </section>

        {/* ── Footer ── */}
        <footer className="max-w-6xl mx-auto px-6 py-8 border-t border-gray-800 text-center text-text-muted text-sm">
          <p>© 2026 PixelPlay. All rights reserved.</p>
        </footer>
      </div>
    </>
  );
}

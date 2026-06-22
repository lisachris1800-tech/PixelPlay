/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#6C63FF',
        'primary-dark': '#5A52D5',
        secondary: '#00D9FF',
        surface: '#121212',
        bg: '#0D0D0D',
        card: '#1A1A2E',
        'text-muted': '#8080A0',
      },
    },
  },
  plugins: [],
}

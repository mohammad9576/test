import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        kodomo: { DEFAULT: '#2E7D5B', dark: '#1F5C42', light: '#5FB98A', mint: '#E8F5EE', cream: '#FAFAF5', ink: '#1B2B26' },
      },
      fontFamily: { sans: ['Vazirmatn', 'system-ui', 'sans-serif'] },
    },
  },
  plugins: [],
};

export default config;

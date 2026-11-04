/** @type {import('next').NextConfig} */
const apiTarget = process.env.API_PROXY_TARGET; // e.g. http://api:3000 – lets the panel call the API on its own origin (/api/v1/...)

const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,
  async rewrites() {
    return apiTarget ? [{ source: '/api/:path*', destination: `${apiTarget.replace(/\/$/, '')}/:path*` }] : [];
  },
};

export default nextConfig;

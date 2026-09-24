import type { NextConfig } from "next";

// Static export only (DEC-67, LLD section 6.6). trailingSlash writes join/index.html and so on, which Nginx
// serves without a redirect. Browser targets are in package.json's browserslist (DEC-111).
const nextConfig: NextConfig = {
  output: "export",
  trailingSlash: true,
  images: { unoptimized: true },
  poweredByHeader: false,
};

export default nextConfig;

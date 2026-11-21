import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Runs Tailwind CSS v4 on every stylesheet
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },

  // The browser only talks to this origin; /api/* is forwarded to the Spring Boot backend
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${process.env.BACKEND_URL}/api/:path*` }];
  },
};

export default nextConfig;

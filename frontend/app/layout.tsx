import type { Metadata } from "next";
import localFont from "next/font/local";
import type { ReactNode } from "react";
import "./globals.css";

// Self-hosted display font (DEC-107, DEC-167), licensed under the SIL Open Font License (app/fonts/OFL.txt)
const pressStart2p = localFont({
  src: "./fonts/PressStart2P-Regular.woff2",
  variable: "--font-press-start-2p",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Delivery Hero",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en" className={pressStart2p.variable}>
      <body className="bg-bg font-sans text-text">{children}</body>
    </html>
  );
}

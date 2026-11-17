import type { Metadata } from "next";
import "./globals.css";
import Providers from "@/components/Providers";

export const metadata: Metadata = {
  title: "DTEF | Digital Funding Ecosystem",
  description: "Accessible tertiary education funding services — demonstration prototype",
  applicationName: "DTEF Digital Funding Ecosystem"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="en"><body><a className="skipLink" href="#main-content">Skip to main content</a><div id="main-content" tabIndex={-1}><Providers>{children}</Providers></div><footer className="globalFooter">DTEF Digital Funding Ecosystem · Botswana government-service concept prototype · Not an official government service</footer></body></html>;
}

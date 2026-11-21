import type { Metadata, Viewport } from "next";
import { Schibsted_Grotesk } from "next/font/google";
import Link from "next/link";
import { NavLinks } from "@/components/NavLinks";
import { ThemeToggle } from "@/components/ThemeToggle";
import "./globals.css";

const schibsted = Schibsted_Grotesk({
  variable: "--font-schibsted",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: { default: "Optimal Portfolio", template: "%s | Optimal Portfolio" },
  description: "Build risk/reward balanced portfolios with Markowitz optimization and constraint programming.",
  // The app has its own dark theme. This tag tells the Dark Reader extension to leave the page alone,
  // instead of rewriting colours (and every SVG) before React hydrates.
  other: { "darkreader-lock": "true" },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

/**
 * Applies the saved theme before the first paint, so a page never flashes in the wrong theme.
 * Kept tiny and dependency-free because it runs before anything else.
 */
const THEME_SCRIPT = `try{var t=localStorage.getItem("theme");if(t==="light"||t==="dark")document.documentElement.dataset.theme=t}catch(e){}`;

/** A small efficient-frontier curve with the chosen portfolio marked: the mark of the product. */
function Logo() {
  return (
    <svg viewBox="0 0 24 24" className="size-6 shrink-0" aria-hidden="true">
      <path d="M3 21 L3 3 M3 21 L21 21" stroke="currentColor" strokeOpacity="0.35" strokeWidth="1.5" fill="none" />
      <path d="M6 18 C 8 10, 12 7, 20 5" stroke="var(--steel)" strokeWidth="2" fill="none" strokeLinecap="round" />
      <circle cx="11.2" cy="9.6" r="2.4" fill="var(--signal)" />
    </svg>
  );
}

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    // The theme script and browser extensions change attributes of <html> and <body> before React hydrates;
    // suppressHydrationWarning ignores those attribute differences on these two elements only.
    <html lang="en" className={`${schibsted.variable} h-full antialiased`} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_SCRIPT }} />
      </head>
      <body className="flex min-h-full flex-col" suppressHydrationWarning>
        <header className="border-b border-rule bg-paper">
          {/* On phones: logo and theme switcher on the first row, navigation on its own row below */}
          <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-8 px-4 sm:flex-nowrap sm:px-6">
            <Link href="/" className="flex items-center gap-2.5 py-3 text-[16px] font-bold tracking-[-0.01em]">
              <Logo />
              Optimal Portfolio
            </Link>
            <nav aria-label="Main" className="order-last -mx-3 w-[calc(100%+1.5rem)] overflow-x-auto sm:order-none sm:mx-0 sm:w-auto">
              <NavLinks />
            </nav>
            <div className="ml-auto">
              <ThemeToggle />
            </div>
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-8 pb-16 sm:px-6 sm:pt-12 sm:pb-20">{children}</main>
      </body>
    </html>
  );
}

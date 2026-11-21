"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const LINKS = [
  { href: "/", label: "Overview" },
  { href: "/assets", label: "Assets" },
  { href: "/optimize", label: "Optimize" },
  { href: "/portfolios", label: "Portfolios" },
];

export function NavLinks() {
  const pathname = usePathname();

  return (
    <ul className="flex gap-1 whitespace-nowrap text-[14px]">
      {LINKS.map((link) => {
        const active = link.href === "/" ? pathname === "/" : pathname.startsWith(link.href);
        return (
          <li key={link.href}>
            <Link
              href={link.href}
              aria-current={active ? "page" : undefined}
              className={`block border-b-2 px-3 py-3 transition-colors sm:py-4 ${
                active ? "border-steel font-semibold text-ink" : "border-transparent text-muted hover:text-ink"
              }`}
            >
              {link.label}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

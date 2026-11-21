"use client";

import { useSyncExternalStore } from "react";

type Theme = "system" | "light" | "dark";

const STORAGE_KEY = "theme";
const CHANGE_EVENT = "themechange";

/*
 * The theme lives on <html data-theme="..."> (set before the first paint by the script in the root layout).
 * useSyncExternalStore reads it from there, so the switcher always shows what the page really uses.
 */
function subscribe(onChange: () => void) {
  window.addEventListener(CHANGE_EVENT, onChange);
  return () => window.removeEventListener(CHANGE_EVENT, onChange);
}

function currentTheme(): Theme {
  const theme = document.documentElement.dataset.theme;
  return theme === "light" || theme === "dark" ? theme : "system";
}

function serverTheme(): Theme {
  return "system";
}

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  if (theme === "system") {
    delete root.dataset.theme;
  } else {
    root.dataset.theme = theme;
  }
  try {
    if (theme === "system") {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, theme);
    }
  } catch {
    // Storage can be unavailable (private mode, blocked cookies); the choice then lasts for this page only
  }
  window.dispatchEvent(new Event(CHANGE_EVENT));
}

const OPTIONS: { theme: Theme; label: string; icon: React.ReactNode }[] = [
  {
    theme: "system",
    label: "Use the system theme",
    icon: (
      <>
        <rect x="3" y="4" width="18" height="12" rx="1.5" />
        <path d="M8 20h8M12 16v4" />
      </>
    ),
  },
  {
    theme: "light",
    label: "Use the light theme",
    icon: (
      <>
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
      </>
    ),
  },
  {
    theme: "dark",
    label: "Use the dark theme",
    icon: <path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z" />,
  },
];

export function ThemeToggle() {
  const theme = useSyncExternalStore(subscribe, currentTheme, serverTheme);

  return (
    <div role="group" aria-label="Theme" className="flex rounded-md border border-rule p-0.5">
      {OPTIONS.map((option) => {
        const active = option.theme === theme;
        return (
          <button
            key={option.theme}
            type="button"
            onClick={() => applyTheme(option.theme)}
            aria-pressed={active}
            aria-label={option.label}
            title={option.label}
            className={`rounded p-1.5 ${active ? "bg-steel-soft text-ink" : "text-muted hover:text-ink"}`}
          >
            <svg
              viewBox="0 0 24 24"
              className="size-4"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.75"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              {option.icon}
            </svg>
          </button>
        );
      })}
    </div>
  );
}

const percentFormat = new Intl.NumberFormat("en-US", {
  style: "percent",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const dateFormat = new Intl.DateTimeFormat("en-US", { year: "numeric", month: "short", day: "numeric" });

export function formatMoney(value: number, currency = "USD"): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(value);
}

export function formatPercent(fraction: number): string {
  return percentFormat.format(fraction);
}

/** Formats an ISO date ("2026-11-20") without shifting it to the local time zone. */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  return dateFormat.format(new Date(Date.UTC(year, month - 1, day)));
}

/** Formats an ISO timestamp ("2026-11-21T16:40:30Z") as a date. */
export function formatTimestamp(isoTimestamp: string): string {
  return dateFormat.format(new Date(isoTimestamp));
}

/** "+1.25%" / "-0.40%": a signed percentage, for changes. */
export function formatSignedPercent(fraction: number): string {
  return `${fraction > 0 ? "+" : ""}${formatPercent(fraction)}`;
}

/** "+$12.40" / "-$3.10": a signed amount of money, for changes. */
export function formatSignedMoney(value: number, currency = "USD"): string {
  return `${value > 0 ? "+" : ""}${formatMoney(value, currency)}`;
}

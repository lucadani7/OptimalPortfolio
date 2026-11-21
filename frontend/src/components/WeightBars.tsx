import { formatPercent } from "@/lib/format";

export interface WeightRow {
  symbol: string;
  weight: number;
}

/** Horizontal bars of portfolio weights, largest first. Assets with a zero weight are left out. */
export function WeightBars({ rows }: { rows: WeightRow[] }) {
  const visible = rows.filter((row) => row.weight > 1e-6).sort((a, b) => b.weight - a.weight);
  const largest = Math.max(...visible.map((row) => row.weight), 0);

  return (
    <ul className="space-y-2.5">
      {visible.map((row) => (
        <li key={row.symbol} className="grid grid-cols-[4rem_1fr_4rem] items-center gap-4 text-[14px]">
          <span className="font-semibold tracking-wide">{row.symbol}</span>
          <div className="h-3 bg-steel-soft">
            <div className="h-full bg-steel" style={{ width: `${(row.weight / largest) * 100}%` }} />
          </div>
          <span className="text-right tabular-nums">{formatPercent(row.weight)}</span>
        </li>
      ))}
    </ul>
  );
}

import type { AssetType } from "@/lib/types";

/** Each kind of asset keeps its own colour everywhere it appears. */
const KINDS: Record<AssetType, { label: string; color: string }> = {
  STOCK: { label: "Stock", color: "var(--steel)" },
  ETF: { label: "ETF", color: "var(--gain)" },
  BOND: { label: "Bond", color: "var(--signal)" },
};

export function AssetTypeBadge({ type }: { type: AssetType }) {
  const kind = KINDS[type];
  return (
    <span className="inline-flex items-center gap-1.5 text-[13px] text-muted">
      <span className="size-2 rounded-full" style={{ backgroundColor: kind.color }} aria-hidden="true" />
      {kind.label}
    </span>
  );
}

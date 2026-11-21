import type { AssetType } from "@/lib/types";

const STYLES: Record<AssetType, string> = {
  STOCK: "bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-300",
  ETF: "bg-violet-100 text-violet-800 dark:bg-violet-950 dark:text-violet-300",
  BOND: "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300",
};

export function AssetTypeBadge({ type }: { type: AssetType }) {
  return (
    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${STYLES[type]}`}>{type}</span>
  );
}

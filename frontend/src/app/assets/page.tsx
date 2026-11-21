import type { Metadata } from "next";
import Link from "next/link";
import { AssetTypeBadge } from "@/components/AssetTypeBadge";
import { apiGet } from "@/lib/api";
import { formatDate, formatPercent } from "@/lib/format";
import type { Asset, AssetType } from "@/lib/types";

export const metadata: Metadata = { title: "Assets" };

const FILTERS: { label: string; type?: AssetType }[] = [
  { label: "All" },
  { label: "Stocks", type: "STOCK" },
  { label: "ETFs", type: "ETF" },
  { label: "Bonds", type: "BOND" },
];

function parseType(value: string | string[] | undefined): AssetType | undefined {
  return value === "STOCK" || value === "ETF" || value === "BOND" ? value : undefined;
}

/** The detail that matters most for each kind of asset. */
function details(asset: Asset): string {
  switch (asset.type) {
    case "STOCK":
      return asset.exchange ?? "—";
    case "ETF":
      return asset.expenseRatio !== undefined ? `Expense ratio ${formatPercent(asset.expenseRatio)}` : "—";
    case "BOND": {
      const coupon = asset.couponRate !== undefined ? `Coupon ${formatPercent(asset.couponRate)}` : "Zero coupon";
      return asset.maturityDate ? `${coupon}, matures ${formatDate(asset.maturityDate)}` : coupon;
    }
  }
}

export default async function AssetsPage({
  searchParams,
}: {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}) {
  const type = parseType((await searchParams).type);
  const assets = await apiGet<Asset[]>(type ? `/api/assets?type=${type}` : "/api/assets");

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <h1 className="text-2xl font-semibold tracking-tight">Assets</h1>
        <div className="flex gap-1 rounded-lg bg-zinc-200/60 p-1 text-sm dark:bg-zinc-800">
          {FILTERS.map((filter) => {
            const active = filter.type === type;
            return (
              <Link
                key={filter.label}
                href={filter.type ? `/assets?type=${filter.type}` : "/assets"}
                className={`rounded-md px-3 py-1.5 ${
                  active
                    ? "bg-white font-medium shadow-sm dark:bg-zinc-950"
                    : "text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
                }`}
              >
                {filter.label}
              </Link>
            );
          })}
        </div>
      </div>

      {assets.length === 0 ? (
        <p className="rounded-xl border border-dashed border-zinc-300 p-10 text-center text-zinc-500 dark:border-zinc-700">
          No assets yet.
        </p>
      ) : (
        <div className="overflow-hidden rounded-xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-zinc-200 bg-zinc-50 text-zinc-500 dark:border-zinc-800 dark:bg-zinc-900/50">
              <tr>
                <th className="px-4 py-3 font-medium">Symbol</th>
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Type</th>
                <th className="px-4 py-3 font-medium">Sector</th>
                <th className="px-4 py-3 font-medium">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
              {assets.map((asset) => (
                <tr key={asset.id} className="hover:bg-zinc-50 dark:hover:bg-zinc-800/50">
                  <td className="px-4 py-3 font-mono font-medium">
                    <Link href={`/assets/${asset.symbol}`} className="text-sky-700 hover:underline dark:text-sky-400">
                      {asset.symbol}
                    </Link>
                  </td>
                  <td className="px-4 py-3">{asset.name}</td>
                  <td className="px-4 py-3">
                    <AssetTypeBadge type={asset.type} />
                  </td>
                  <td className="px-4 py-3 text-zinc-600 dark:text-zinc-400">{asset.sector}</td>
                  <td className="px-4 py-3 text-zinc-600 dark:text-zinc-400">{details(asset)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

import Link from "next/link";
import { apiGet } from "@/lib/api";
import type { Asset, AssetType } from "@/lib/types";

const TYPE_LABELS: Record<AssetType, string> = {
  STOCK: "Stocks",
  ETF: "ETFs",
  BOND: "Bonds",
};

export default async function OverviewPage() {
  const assets = await apiGet<Asset[]>("/api/assets");
  const counts = (Object.keys(TYPE_LABELS) as AssetType[]).map((type) => ({
    type,
    count: assets.filter((asset) => asset.type === type).length,
  }));

  return (
    <div className="space-y-10">
      <section className="space-y-3">
        <h1 className="text-3xl font-semibold tracking-tight">Optimal Portfolio</h1>
        <p className="max-w-2xl text-zinc-600 dark:text-zinc-400">
          Build risk/reward balanced portfolios: Markowitz optimization finds the target weights, and
          constraint programming turns them into whole shares for your budget.
        </p>
      </section>

      <section className="grid gap-4 sm:grid-cols-3">
        {counts.map(({ type, count }) => (
          <Link
            key={type}
            href={`/assets?type=${type}`}
            className="rounded-xl border border-zinc-200 bg-white p-6 transition hover:border-zinc-400 dark:border-zinc-800 dark:bg-zinc-900 dark:hover:border-zinc-600"
          >
            <p className="text-sm text-zinc-500">{TYPE_LABELS[type]}</p>
            <p className="mt-2 text-3xl font-semibold">{count}</p>
          </Link>
        ))}
      </section>

      <Link href="/assets" className="inline-flex text-sm font-medium text-sky-700 hover:underline dark:text-sky-400">
        Browse all {assets.length} assets →
      </Link>
    </div>
  );
}

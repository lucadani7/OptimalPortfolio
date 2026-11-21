import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { AssetTypeBadge } from "@/components/AssetTypeBadge";
import { PriceChart } from "@/components/PriceChart";
import { ApiError, apiGet } from "@/lib/api";
import { formatDate, formatMoney, formatPercent } from "@/lib/format";
import type { Asset, PricePoint } from "@/lib/types";

const RANGES = [
  { key: "1M", months: 1 },
  { key: "3M", months: 3 },
  { key: "6M", months: 6 },
  { key: "1Y", months: 12 },
  { key: "3Y", months: 36 },
  { key: "ALL", months: undefined },
] as const;

type RangeKey = (typeof RANGES)[number]["key"];

const DEFAULT_RANGE: RangeKey = "1Y";

interface AssetPageProps {
  params: Promise<{ symbol: string }>;
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}

function parseRange(value: string | string[] | undefined): RangeKey {
  return RANGES.find((range) => range.key === value)?.key ?? DEFAULT_RANGE;
}

/** ISO date ("2026-11-20") of today minus the given number of months, in UTC. */
function monthsAgo(months: number): string {
  const date = new Date();
  date.setUTCMonth(date.getUTCMonth() - months);
  return date.toISOString().slice(0, 10);
}

async function getAsset(symbol: string): Promise<Asset> {
  try {
    return await apiGet<Asset>(`/api/assets/${encodeURIComponent(symbol)}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}

export async function generateMetadata({ params }: AssetPageProps): Promise<Metadata> {
  const { symbol } = await params;
  return { title: symbol.toUpperCase() };
}

export default async function AssetPage({ params, searchParams }: AssetPageProps) {
  const { symbol } = await params;
  const rangeKey = parseRange((await searchParams).range);
  const range = RANGES.find((r) => r.key === rangeKey)!;

  const asset = await getAsset(symbol);
  const query = range.months !== undefined ? `?from=${monthsAgo(range.months)}` : "";
  const prices = await apiGet<PricePoint[]>(`/api/assets/${encodeURIComponent(asset.symbol)}/prices${query}`);

  const first = prices.at(0);
  const last = prices.at(-1);
  const change = first && last ? last.close - first.close : undefined;
  const closes = prices.map((price) => price.close);

  const facts: { label: string; value: string }[] = [
    { label: "Sector", value: asset.sector },
    { label: "Currency", value: asset.currency },
  ];
  if (asset.exchange) facts.push({ label: "Exchange", value: asset.exchange });
  if (asset.expenseRatio !== undefined) facts.push({ label: "Expense ratio", value: formatPercent(asset.expenseRatio) });
  if (asset.couponRate !== undefined) facts.push({ label: "Coupon", value: formatPercent(asset.couponRate) });
  if (asset.maturityDate) facts.push({ label: "Maturity", value: formatDate(asset.maturityDate) });

  return (
    <div className="space-y-8">
      <Link href="/assets" className="text-sm text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100">
        ← All assets
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <h1 className="font-mono text-3xl font-semibold">{asset.symbol}</h1>
            <AssetTypeBadge type={asset.type} />
          </div>
          <p className="text-zinc-600 dark:text-zinc-400">{asset.name}</p>
        </div>

        {last && (
          <div className="text-right">
            <p className="text-3xl font-semibold">{formatMoney(last.close, asset.currency)}</p>
            {first && change !== undefined && (
              <p className={change >= 0 ? "text-emerald-600" : "text-red-600"}>
                {change >= 0 ? "+" : ""}
                {formatMoney(change, asset.currency)} ({formatPercent(change / first.close)}) over {rangeKey}
              </p>
            )}
            <p className="text-xs text-zinc-500">Close on {formatDate(last.date)}</p>
          </div>
        )}
      </div>

      <section className="space-y-4 rounded-xl border border-zinc-200 bg-white p-6 dark:border-zinc-800 dark:bg-zinc-900">
        <div className="flex justify-end gap-1 text-sm">
          {RANGES.map((r) => (
            <Link
              key={r.key}
              href={`/assets/${asset.symbol}?range=${r.key}`}
              scroll={false}
              className={`rounded-md px-2.5 py-1 ${
                r.key === rangeKey
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
                  : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
              }`}
            >
              {r.key}
            </Link>
          ))}
        </div>
        <PriceChart prices={prices} currency={asset.currency} />
        {closes.length > 0 && (
          <div className="flex gap-8 text-sm text-zinc-500">
            <span>High {formatMoney(Math.max(...closes), asset.currency)}</span>
            <span>Low {formatMoney(Math.min(...closes), asset.currency)}</span>
            <span>{prices.length} trading days</span>
          </div>
        )}
      </section>

      <dl className="grid gap-4 sm:grid-cols-3">
        {facts.map((fact) => (
          <div
            key={fact.label}
            className="rounded-xl border border-zinc-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900"
          >
            <dt className="text-sm text-zinc-500">{fact.label}</dt>
            <dd className="mt-1 font-medium">{fact.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { AssetTypeBadge } from "@/components/AssetTypeBadge";
import { PriceChart } from "@/components/PriceChart";
import { ApiError, apiGet } from "@/lib/api";
import { formatDate, formatMoney, formatPercent } from "@/lib/format";
import type { Asset, PricePoint } from "@/lib/types";

const RANGES = [
  { key: "1M", label: "1 month", months: 1 },
  { key: "3M", label: "3 months", months: 3 },
  { key: "6M", label: "6 months", months: 6 },
  { key: "1Y", label: "1 year", months: 12 },
  { key: "3Y", label: "3 years", months: 36 },
  { key: "ALL", label: "All history", months: undefined },
] as const;

type Range = (typeof RANGES)[number];

const DEFAULT_RANGE = RANGES[3];

interface AssetPageProps {
  params: Promise<{ symbol: string }>;
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}

function parseRange(value: string | string[] | undefined): Range {
  return RANGES.find((range) => range.key === value) ?? DEFAULT_RANGE;
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
  const range = parseRange((await searchParams).range);

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
  if (asset.expenseRatio !== undefined) facts.push({ label: "Yearly fee", value: formatPercent(asset.expenseRatio) });
  if (asset.couponRate !== undefined) facts.push({ label: "Coupon", value: formatPercent(asset.couponRate) });
  if (asset.maturityDate) facts.push({ label: "Matures", value: formatDate(asset.maturityDate) });
  if (closes.length > 0) {
    facts.push({ label: `High, ${range.label.toLowerCase()}`, value: formatMoney(Math.max(...closes), asset.currency) });
    facts.push({ label: `Low, ${range.label.toLowerCase()}`, value: formatMoney(Math.min(...closes), asset.currency) });
  }

  return (
    <div className="space-y-10">
      <Link href="/assets" className="text-[14px] text-muted hover:text-ink">
        Back to all assets
      </Link>

      <div className="flex flex-wrap items-end justify-between gap-8">
        <div className="space-y-2">
          <div className="flex items-baseline gap-4">
            <h1 className="text-[34px] leading-none font-extrabold tracking-[-0.02em] sm:text-[44px]">{asset.symbol}</h1>
            <AssetTypeBadge type={asset.type} />
          </div>
          <p className="text-[17px] text-muted">{asset.name}</p>
        </div>

        {last && (
          <div className="space-y-1 sm:text-right">
            <p className="text-[28px] leading-none font-bold tracking-[-0.02em] tabular-nums sm:text-[35px]">
              {formatMoney(last.close, asset.currency)}
            </p>
            {first && change !== undefined && (
              <p className={`text-[15px] font-semibold ${change >= 0 ? "text-gain" : "text-loss"}`}>
                {change >= 0 ? "+" : ""}
                {formatMoney(change, asset.currency)} ({formatPercent(change / first.close)}) in{" "}
                {range.label.toLowerCase()}
              </p>
            )}
            <p className="text-[13px] text-muted">Closing price on {formatDate(last.date)}</p>
          </div>
        )}
      </div>

      <section className="graph-paper -mx-4 border-y border-rule sm:mx-0 sm:border">
        <nav aria-label="Period" className="flex flex-wrap justify-end gap-1 border-b border-rule bg-panel px-3 py-2 text-[13px]">
          {RANGES.map((r) => (
            <Link
              key={r.key}
              href={`/assets/${asset.symbol}?range=${r.key}`}
              scroll={false}
              aria-current={r.key === range.key ? "page" : undefined}
              title={r.label}
              className={`rounded px-2.5 py-1 font-semibold ${
                r.key === range.key ? "bg-steel text-paper" : "text-muted hover:text-ink"
              }`}
            >
              {r.key}
            </Link>
          ))}
        </nav>
        <div className="p-2 sm:p-6">
          <PriceChart prices={prices} currency={asset.currency} />
        </div>
      </section>

      <dl className="grid grid-cols-2 gap-x-8 gap-y-5 sm:grid-cols-4">
        {facts.map((fact) => (
          <div key={fact.label} className="border-t border-rule pt-3">
            <dt className="text-[13px] text-muted">{fact.label}</dt>
            <dd className="mt-0.5 text-[16px] font-semibold">{fact.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

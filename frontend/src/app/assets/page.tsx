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
      return asset.exchange ? `Listed on ${asset.exchange}` : "";
    case "ETF":
      return asset.expenseRatio !== undefined ? `${formatPercent(asset.expenseRatio)} yearly fee` : "";
    case "BOND": {
      const coupon = asset.couponRate !== undefined ? `${formatPercent(asset.couponRate)} coupon` : "Zero coupon";
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
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-6">
        <h1 className="text-[28px] leading-tight font-extrabold tracking-[-0.025em] sm:text-[35px]">Assets</h1>
        <nav aria-label="Filter by kind" className="flex gap-5 text-[14px]">
          {FILTERS.map((filter) => {
            const active = filter.type === type;
            return (
              <Link
                key={filter.label}
                href={filter.type ? `/assets?type=${filter.type}` : "/assets"}
                aria-current={active ? "page" : undefined}
                className={`border-b-2 pb-1 ${active ? "border-steel font-semibold" : "border-transparent text-muted hover:text-ink"}`}
              >
                {filter.label}
              </Link>
            );
          })}
        </nav>
      </div>

      {assets.length === 0 ? (
        <p className="border border-dashed border-rule p-10 text-muted">
          There are no assets of this kind yet. Create one through the API, then import its prices.
        </p>
      ) : (
        <table className="w-full text-left text-[14px]">
          <thead className="border-b-2 border-ink text-[13px] text-muted">
            <tr>
              <th className="py-2.5 pr-4 font-medium">Symbol</th>
              <th className="py-2.5 pr-4 font-medium">Name</th>
              <th className="py-2.5 pr-4 font-medium">Kind</th>
              <th className="hidden py-2.5 pr-4 font-medium md:table-cell">Sector</th>
              <th className="hidden py-2.5 font-medium lg:table-cell">Details</th>
            </tr>
          </thead>
          <tbody>
            {assets.map((asset) => (
              <tr key={asset.id} className="border-b border-rule hover:bg-panel">
                <td className="py-3 pr-4">
                  <Link href={`/assets/${asset.symbol}`} className="font-bold tracking-wide text-steel hover:underline">
                    {asset.symbol}
                  </Link>
                </td>
                <td className="py-3 pr-4">{asset.name}</td>
                <td className="py-3 pr-4">
                  <AssetTypeBadge type={asset.type} />
                </td>
                <td className="hidden py-3 pr-4 text-muted md:table-cell">{asset.sector}</td>
                <td className="hidden py-3 text-muted lg:table-cell">{details(asset)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

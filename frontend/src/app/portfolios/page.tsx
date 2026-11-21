import type { Metadata } from "next";
import Link from "next/link";
import { apiGet } from "@/lib/api";
import { formatMoney, formatPercent, formatTimestamp } from "@/lib/format";
import { strategyLabel } from "@/lib/strategies";
import type { PortfolioSummary } from "@/lib/types";

export const metadata: Metadata = { title: "Portfolios" };

export default async function PortfoliosPage() {
  const portfolios = await apiGet<PortfolioSummary[]>("/api/portfolios");

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <h1 className="text-[28px] leading-tight font-extrabold tracking-[-0.025em] sm:text-[35px]">Portfolios</h1>
        <Link
          href="/optimize"
          className="rounded-md bg-steel px-4 py-2 text-[14px] font-semibold text-paper hover:opacity-90"
        >
          Build a new portfolio
        </Link>
      </div>

      {portfolios.length === 0 ? (
        <div className="graph-paper space-y-3 border border-rule p-10">
          <p className="font-semibold">You have no saved portfolios yet</p>
          <p className="max-w-md text-muted">
            Optimize a set of assets, allocate a budget in whole shares, then save the result to follow its value
            here.
          </p>
        </div>
      ) : (
        <table className="w-full text-left text-[14px]">
          <thead className="border-b-2 border-ink text-[13px] text-muted">
            <tr>
              <th className="py-2.5 pr-4 font-medium">Name</th>
              <th className="hidden py-2.5 pr-4 font-medium md:table-cell">Strategy</th>
              <th className="py-2.5 pr-4 text-right font-medium">Budget</th>
              <th className="hidden py-2.5 pr-4 text-right font-medium sm:table-cell">Assets</th>
              <th className="py-2.5 pr-4 text-right font-medium">Expected return</th>
              <th className="hidden py-2.5 pr-4 text-right font-medium lg:table-cell">Sharpe</th>
              <th className="hidden py-2.5 text-right font-medium md:table-cell">Created</th>
            </tr>
          </thead>
          <tbody className="tabular-nums">
            {portfolios.map((portfolio) => (
              <tr key={portfolio.id} className="border-b border-rule hover:bg-panel">
                <td className="py-3 pr-4">
                  <Link href={`/portfolios/${portfolio.id}`} className="font-semibold text-steel hover:underline">
                    {portfolio.name}
                  </Link>
                </td>
                <td className="hidden py-3 pr-4 text-muted md:table-cell">{strategyLabel(portfolio.strategy)}</td>
                <td className="py-3 pr-4 text-right">{formatMoney(portfolio.budget)}</td>
                <td className="hidden py-3 pr-4 text-right sm:table-cell">{portfolio.holdings}</td>
                <td className="py-3 pr-4 text-right">{formatPercent(portfolio.expectedReturn)}</td>
                <td className="hidden py-3 pr-4 text-right lg:table-cell">{portfolio.sharpeRatio.toFixed(2)}</td>
                {/* Dates are text, not figures to compare: proportional digits keep "Nov 21, 2026" tight */}
                <td className="hidden py-3 text-right text-muted [font-variant-numeric:normal] md:table-cell">
                  {formatTimestamp(portfolio.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

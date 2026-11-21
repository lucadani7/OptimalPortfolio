import Link from "next/link";
import { FrontierChart } from "@/components/FrontierChart";
import { apiGet, apiPost } from "@/lib/api";
import { formatPercent } from "@/lib/format";
import type { Asset, Frontier, FrontierRequest } from "@/lib/types";

const RISK_FREE_RATE = 0.04;

async function loadFrontier(symbols: string[]): Promise<Frontier | null> {
  if (symbols.length < 2) return null;
  const request: FrontierRequest = { symbols, riskFreeRate: RISK_FREE_RATE, points: 40 };
  try {
    return await apiPost<Frontier>("/api/optimization/frontier", request);
  } catch {
    // The overview still works without the chart, e.g. when there is not enough price history yet
    return null;
  }
}

export default async function OverviewPage() {
  const assets = await apiGet<Asset[]>("/api/assets");
  const frontier = await loadFrontier(assets.map((asset) => asset.symbol));
  const best = frontier?.points.reduce((a, b) => (b.sharpeRatio > a.sharpeRatio ? b : a));

  return (
    <div className="space-y-12">
      <section className="max-w-[44rem] space-y-4">
        <h1 className="text-[32px] leading-[1.08] font-extrabold tracking-[-0.03em] text-balance sm:text-[44px] sm:leading-[1.05]">
          {assets.length} assets, plotted by risk and return
        </h1>
        <p className="text-[16px] text-muted sm:text-[17px]">
          Each grey dot is one asset over the last year. The blue curve is the efficient frontier: the highest
          return you can expect for each level of risk by combining them. The marked point earns the most per unit
          of risk.
        </p>
      </section>

      {frontier && best ? (
        <section className="graph-paper -mx-4 border-y border-rule p-2 sm:mx-0 sm:border sm:p-6">
          <FrontierChart
            frontier={frontier}
            className="h-80 sm:h-[460px]"
            highlight={{ volatility: best.volatility, expectedReturn: best.expectedReturn, label: "Best trade-off" }}
          />
          <p className="mt-2 px-2 text-[13px] text-muted">
            Best trade-off: {formatPercent(best.expectedReturn)} expected return at {formatPercent(best.volatility)}{" "}
            volatility, a Sharpe ratio of {best.sharpeRatio.toFixed(2)} with a {formatPercent(RISK_FREE_RATE)}{" "}
            risk-free rate.
          </p>
        </section>
      ) : (
        <p className="border border-dashed border-rule p-10 text-muted">
          The frontier needs at least two assets with a year of shared price history. Add assets and import their
          prices to see it.
        </p>
      )}

      <div className="flex flex-wrap gap-3">
        <Link
          href="/optimize"
          className="rounded-md bg-steel px-5 py-2.5 text-[15px] font-semibold text-paper hover:opacity-90"
        >
          Optimize a portfolio
        </Link>
        <Link
          href="/assets"
          className="rounded-md border border-rule px-5 py-2.5 text-[15px] font-semibold hover:border-ink"
        >
          See all assets
        </Link>
      </div>
    </div>
  );
}

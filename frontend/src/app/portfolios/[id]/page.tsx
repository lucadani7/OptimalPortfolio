import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { PortfolioActions } from "@/components/PortfolioActions";
import { WeightBars } from "@/components/WeightBars";
import { ApiError, apiGet } from "@/lib/api";
import { describeConstraint } from "@/lib/constraints";
import {
  formatDate,
  formatMoney,
  formatPercent,
  formatSignedMoney,
  formatSignedPercent,
  formatTimestamp,
} from "@/lib/format";
import { strategyLabel } from "@/lib/strategies";
import type { Holding, Portfolio } from "@/lib/types";

interface PortfolioPageProps {
  params: Promise<{ id: string }>;
}

async function getPortfolio(id: string): Promise<Portfolio> {
  if (!/^\d+$/.test(id)) notFound();
  try {
    return await apiGet<Portfolio>(`/api/portfolios/${id}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}

export async function generateMetadata({ params }: PortfolioPageProps): Promise<Metadata> {
  const { id } = await params;
  try {
    return { title: (await getPortfolio(id)).name };
  } catch {
    return { title: "Portfolio" };
  }
}

/** Market value of a holding, or its cost when no current price is known. */
function value(holding: Holding): number {
  return holding.currentValue ?? holding.cost;
}

function tone(amount: number): string {
  if (amount > 0) return "text-gain";
  if (amount < 0) return "text-loss";
  return "";
}

export default async function PortfolioPage({ params }: PortfolioPageProps) {
  const { id } = await params;
  const portfolio = await getPortfolio(id);
  const { valuation } = portfolio;
  const holdingsValue = portfolio.holdings.reduce((sum, holding) => sum + value(holding), 0);

  return (
    <div className="space-y-12">
      <div className="space-y-6">
        <Link href="/portfolios" className="text-[14px] text-muted hover:text-ink">
          Back to all portfolios
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-6">
          <div className="space-y-1.5">
            <h1 className="text-[28px] leading-tight font-extrabold tracking-[-0.025em] text-balance sm:text-[35px]">
              {portfolio.name}
            </h1>
            <p className="text-muted">
              {strategyLabel(portfolio.strategy)}, created {formatTimestamp(portfolio.createdAt)}
            </p>
          </div>
          <PortfolioActions id={portfolio.id} name={portfolio.name} />
        </div>
      </div>

      <section className="space-y-3">
        <dl className="flex flex-wrap gap-x-10 gap-y-4 border-b-2 border-ink pb-5">
          <div className="flex flex-col-reverse gap-1.5">
            <dt className="text-[13px] text-muted">Value today</dt>
            <dd className="text-[28px] leading-none font-bold tracking-[-0.02em] tabular-nums sm:text-[35px]">
              {formatMoney(valuation.currentValue)}
            </dd>
          </div>
          <div className="flex flex-col-reverse gap-1.5">
            <dt className="text-[13px] text-muted">Gain or loss</dt>
            <dd
              className={`text-[28px] leading-none font-bold tracking-[-0.02em] tabular-nums sm:text-[35px] ${tone(valuation.profitLoss)}`}
            >
              {formatSignedMoney(valuation.profitLoss)}
            </dd>
          </div>
          <div className="flex flex-col-reverse gap-1.5">
            <dt className="text-[13px] text-muted">Return on budget</dt>
            <dd
              className={`text-[28px] leading-none font-bold tracking-[-0.02em] tabular-nums sm:text-[35px] ${tone(valuation.returnRate)}`}
            >
              {formatSignedPercent(valuation.returnRate)}
            </dd>
          </div>
        </dl>
        <p className="text-[14px] text-muted">
          {formatMoney(portfolio.budget)} budget: {formatMoney(portfolio.invested)} in shares and{" "}
          {formatMoney(portfolio.leftover)} kept in cash. Values use the latest known prices.
        </p>
      </section>

      <section className="space-y-4">
        <h2 className="border-b border-rule pb-2 text-[20px] font-bold tracking-[-0.01em]">Holdings</h2>
        <table className="w-full text-left text-[14px]">
          <thead className="border-b-2 border-ink text-[13px] text-muted">
            <tr>
              <th className="py-2 pr-3 font-medium">Asset</th>
              <th className="py-2 pr-3 text-right font-medium">Shares</th>
              <th className="hidden py-2 pr-3 text-right font-medium md:table-cell">Bought at</th>
              <th className="hidden py-2 pr-3 text-right font-medium sm:table-cell">Price now</th>
              <th className="py-2 pr-3 text-right font-medium">Value</th>
              <th className="py-2 text-right font-medium">Gain or loss</th>
            </tr>
          </thead>
          <tbody className="tabular-nums">
            {portfolio.holdings.map((holding) => {
              const change = value(holding) - holding.cost;
              return (
                <tr key={holding.symbol} className="border-b border-rule">
                  <td className="py-2.5 pr-3">
                    <Link href={`/assets/${holding.symbol}`} className="font-semibold tracking-wide text-steel hover:underline">
                      {holding.symbol}
                    </Link>
                    <span className="block text-[13px] text-muted">{holding.name}</span>
                  </td>
                  <td className="py-2.5 pr-3 text-right">{holding.shares}</td>
                  <td className="hidden py-2.5 pr-3 text-right md:table-cell">
                    {formatMoney(holding.purchasePrice)}
                    <span className="block text-[13px] text-muted">{formatDate(holding.priceDate)}</span>
                  </td>
                  <td className="hidden py-2.5 pr-3 text-right sm:table-cell">
                    {holding.currentPrice !== undefined ? formatMoney(holding.currentPrice) : "No price"}
                    {holding.currentPriceDate && (
                      <span className="block text-[13px] text-muted">{formatDate(holding.currentPriceDate)}</span>
                    )}
                  </td>
                  <td className="py-2.5 pr-3 text-right">{formatMoney(value(holding))}</td>
                  <td className={`py-2.5 text-right font-semibold ${tone(change)}`}>
                    {formatSignedMoney(change)}
                    <span className="block text-[13px] font-normal">{formatSignedPercent(change / holding.cost)}</span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>

      <div className="grid gap-12 lg:grid-cols-2">
        <section className="space-y-4">
          <h2 className="border-b border-rule pb-2 text-[20px] font-bold tracking-[-0.01em]">Weights today</h2>
          <WeightBars
            rows={portfolio.holdings.map((holding) => ({
              symbol: holding.symbol,
              weight: holdingsValue > 0 ? value(holding) / (holdingsValue + portfolio.leftover) : 0,
            }))}
          />
          <p className="text-[13px] text-muted">Share of the total value, cash included.</p>
        </section>

        <section className="space-y-4">
          <h2 className="border-b border-rule pb-2 text-[20px] font-bold tracking-[-0.01em]">How it was built</h2>
          <dl className="grid grid-cols-2 gap-x-6 gap-y-4 text-[14px]">
            {[
              { label: "Expected return", value: `${formatPercent(portfolio.expectedReturn)} a year` },
              { label: "Volatility", value: `${formatPercent(portfolio.volatility)} a year` },
              { label: "Sharpe ratio", value: portfolio.sharpeRatio.toFixed(2) },
              { label: "Risk-free rate", value: formatPercent(portfolio.riskFreeRate) },
            ].map((fact) => (
              <div key={fact.label}>
                <dt className="text-[13px] text-muted">{fact.label}</dt>
                <dd className="font-semibold tabular-nums">{fact.value}</dd>
              </div>
            ))}
          </dl>
          <p className="text-[13px] text-muted">
            Estimated from prices between {formatDate(portfolio.statisticsFrom)} and {formatDate(portfolio.statisticsTo)}.
          </p>
          <div className="space-y-1.5">
            <p className="text-[14px] font-semibold">Rules</p>
            {portfolio.constraints.length === 0 ? (
              <p className="text-[14px] text-muted">None: every weight between 0% and 100%.</p>
            ) : (
              <ul className="list-disc space-y-0.5 pl-5 text-[14px] text-muted">
                {portfolio.constraints.map((constraint, index) => (
                  <li key={index}>{describeConstraint(constraint)}</li>
                ))}
              </ul>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

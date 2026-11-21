"use client";

import Link from "next/link";
import { useMemo, useState, useTransition } from "react";
import { FrontierChart } from "@/components/FrontierChart";
import { WeightBars } from "@/components/WeightBars";
import { apiPost } from "@/lib/client-api";
import { formatMoney, formatPercent } from "@/lib/format";
import { strategyLabel } from "@/lib/strategies";
import type {
  AllocationRequest,
  AllocationResult,
  Asset,
  ConstraintRequest,
  CreatePortfolioRequest,
  Frontier,
  FrontierRequest,
  OptimizationRequest,
  OptimizationResult,
  Portfolio,
  Strategy,
} from "@/lib/types";

interface OptimizerProps {
  assets: Asset[];
  strategies: Strategy[];
}

interface SectorLimitInput {
  id: number;
  sector: string;
  maxPercent: string;
}

interface Analysis {
  request: OptimizationRequest;
  result: OptimizationResult;
  frontier: Frontier | null;
  frontierError: string | null;
}

const LOOKBACK_YEARS = [1, 2, 3] as const;

const FRONTIER_POINTS = 30;

/** Parses a percentage typed by the user ("40" means 0.4). Empty input means "not set". */
function parsePercent(text: string, label: string): number | undefined {
  if (text.trim() === "") return undefined;
  const value = Number(text);
  if (!Number.isFinite(value)) throw new Error(`${label} must be a number`);
  return value / 100;
}

function yearsAgo(years: number): string {
  const date = new Date();
  date.setUTCFullYear(date.getUTCFullYear() - years);
  return date.toISOString().slice(0, 10);
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

export function Optimizer({ assets, strategies }: OptimizerProps) {
  const sectors = useMemo(() => [...new Set(assets.map((asset) => asset.sector))].sort(), [assets]);

  const [selected, setSelected] = useState<string[]>(() => assets.map((asset) => asset.symbol));
  const [strategy, setStrategy] = useState(
    () => strategies.find((s) => s.name === "max-sharpe")?.name ?? strategies[0]?.name ?? "",
  );
  const [lookbackYears, setLookbackYears] = useState<number>(1);
  const [riskFreePercent, setRiskFreePercent] = useState("4");
  const [maxWeightPercent, setMaxWeightPercent] = useState("40");
  const [maxAssets, setMaxAssets] = useState("");
  const [minReturnPercent, setMinReturnPercent] = useState("");
  const [sectorLimits, setSectorLimits] = useState<SectorLimitInput[]>([]);
  const [nextLimitId, setNextLimitId] = useState(1);

  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [optimizing, startOptimizing] = useTransition();

  const [budget, setBudget] = useState("10000");
  const [allocation, setAllocation] = useState<AllocationResult | null>(null);
  const [allocationError, setAllocationError] = useState<string | null>(null);
  const [allocating, startAllocating] = useTransition();

  const [portfolioName, setPortfolioName] = useState("");
  const [saved, setSaved] = useState<Portfolio | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saving, startSaving] = useTransition();

  function toggleAsset(symbol: string) {
    setSelected((current) =>
      current.includes(symbol) ? current.filter((s) => s !== symbol) : [...current, symbol],
    );
  }

  function addSectorLimit() {
    setSectorLimits((current) => [...current, { id: nextLimitId, sector: sectors[0] ?? "", maxPercent: "30" }]);
    setNextLimitId((id) => id + 1);
  }

  function updateSectorLimit(id: number, change: Partial<SectorLimitInput>) {
    setSectorLimits((current) => current.map((limit) => (limit.id === id ? { ...limit, ...change } : limit)));
  }

  function removeSectorLimit(id: number) {
    setSectorLimits((current) => current.filter((limit) => limit.id !== id));
  }

  /** Builds the request from the form; throws with a readable message if an input is invalid. */
  function buildRequest(): OptimizationRequest {
    if (selected.length === 0) throw new Error("Select at least one asset");

    const constraints: ConstraintRequest[] = [];
    const maxWeight = parsePercent(maxWeightPercent, "Maximum weight per asset");
    if (maxWeight !== undefined) constraints.push({ type: "WEIGHT_BOUNDS", min: 0, max: maxWeight });
    for (const limit of sectorLimits) {
      const maxSectorWeight = parsePercent(limit.maxPercent, `Limit for ${limit.sector}`);
      if (maxSectorWeight !== undefined) {
        constraints.push({ type: "SECTOR_LIMIT", sector: limit.sector, maxWeight: maxSectorWeight });
      }
    }
    if (maxAssets.trim() !== "") {
      const count = Number(maxAssets);
      if (!Number.isInteger(count) || count < 1) throw new Error("Maximum number of assets must be a whole number");
      constraints.push({ type: "MAX_ASSETS", count });
    }
    const minReturn = parsePercent(minReturnPercent, "Minimum expected return");
    if (minReturn !== undefined) constraints.push({ type: "MIN_EXPECTED_RETURN", value: minReturn });

    return {
      symbols: selected,
      from: yearsAgo(lookbackYears),
      strategy,
      constraints,
      riskFreeRate: parsePercent(riskFreePercent, "Risk-free rate") ?? 0,
    };
  }

  function optimize() {
    setError(null);
    setAllocation(null);
    setSaved(null);
    startOptimizing(async () => {
      try {
        const request = buildRequest();
        const frontierRequest: FrontierRequest = {
          symbols: request.symbols,
          from: request.from,
          constraints: request.constraints,
          riskFreeRate: request.riskFreeRate,
          points: FRONTIER_POINTS,
        };
        // The frontier is optional context: a failure there should not hide the optimization result
        const [result, frontier] = await Promise.allSettled([
          apiPost<OptimizationResult>("/api/optimization/run", request),
          apiPost<Frontier>("/api/optimization/frontier", frontierRequest),
        ]);
        if (result.status === "rejected") throw result.reason;
        setAnalysis({
          request,
          result: result.value,
          frontier: frontier.status === "fulfilled" ? frontier.value : null,
          frontierError: frontier.status === "rejected" ? errorMessage(frontier.reason) : null,
        });
      } catch (e) {
        setAnalysis(null);
        setError(errorMessage(e));
      }
    });
  }

  function allocationRequest(): AllocationRequest {
    if (!analysis) throw new Error("Run an optimization first");
    const amount = Number(budget);
    if (!Number.isFinite(amount) || amount <= 0) throw new Error("Budget must be a positive number");
    return { ...analysis.request, budget: amount };
  }

  function allocate() {
    setAllocationError(null);
    setSaved(null);
    startAllocating(async () => {
      try {
        setAllocation(await apiPost<AllocationResult>("/api/optimization/allocate", allocationRequest()));
      } catch (e) {
        setAllocation(null);
        setAllocationError(errorMessage(e));
      }
    });
  }

  function save() {
    setSaveError(null);
    startSaving(async () => {
      try {
        if (portfolioName.trim() === "") throw new Error("Give the portfolio a name");
        const request: CreatePortfolioRequest = { name: portfolioName.trim(), allocation: allocationRequest() };
        setSaved(await apiPost<Portfolio>("/api/portfolios", request));
      } catch (e) {
        setSaveError(errorMessage(e));
      }
    });
  }

  const inputClass =
    "w-full rounded-md border border-rule bg-panel px-3 py-2 text-[14px] outline-none focus:border-steel";
  const primaryButton =
    "rounded-md bg-steel px-5 py-2.5 text-[15px] font-semibold text-paper hover:opacity-90 disabled:opacity-50";
  const secondaryButton =
    "rounded-md border border-ink px-5 py-2.5 text-[15px] font-semibold hover:bg-panel disabled:opacity-50";
  const fieldLabel = "text-[13px] text-muted";

  return (
    <div className="grid gap-10 lg:grid-cols-[20rem_1fr] lg:gap-12">
      <form
        className="h-fit space-y-8 lg:sticky lg:top-6"
        onSubmit={(event) => {
          event.preventDefault();
          optimize();
        }}
      >
        <fieldset className="space-y-3">
          <div className="flex items-baseline justify-between border-b border-rule pb-2">
            <legend className="text-[16px] font-bold">Assets</legend>
            <div className="flex gap-4 text-[13px] text-steel">
              <button type="button" className="hover:underline" onClick={() => setSelected(assets.map((a) => a.symbol))}>
                Select all
              </button>
              <button type="button" className="hover:underline" onClick={() => setSelected([])}>
                Clear
              </button>
            </div>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {assets.map((asset) => {
              const active = selected.includes(asset.symbol);
              return (
                <button
                  key={asset.symbol}
                  type="button"
                  title={asset.name}
                  aria-pressed={active}
                  onClick={() => toggleAsset(asset.symbol)}
                  className={`rounded px-2.5 py-1 text-[13px] font-semibold tracking-wide ${
                    active ? "bg-steel text-paper" : "bg-steel-soft text-muted hover:text-ink"
                  }`}
                >
                  {asset.symbol}
                </button>
              );
            })}
          </div>
        </fieldset>

        <fieldset className="space-y-4">
          <legend className="w-full border-b border-rule pb-2 text-[16px] font-bold">Strategy</legend>
          <div className="space-y-2">
            {strategies.map((s) => (
              <label key={s.name} className="flex cursor-pointer gap-3">
                <input
                  type="radio"
                  name="strategy"
                  value={s.name}
                  checked={strategy === s.name}
                  onChange={() => setStrategy(s.name)}
                  className="mt-1 accent-[var(--steel)]"
                />
                <span>
                  <span className="block text-[14px] font-semibold">{strategyLabel(s.name)}</span>
                  <span className="block text-[13px] text-muted">{s.description}</span>
                </span>
              </label>
            ))}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <label className="block space-y-1">
              <span className={fieldLabel}>Price history</span>
              <select
                className={inputClass}
                value={lookbackYears}
                onChange={(e) => setLookbackYears(Number(e.target.value))}
              >
                {LOOKBACK_YEARS.map((years) => (
                  <option key={years} value={years}>
                    {years} {years === 1 ? "year" : "years"}
                  </option>
                ))}
              </select>
            </label>
            <label className="block space-y-1">
              <span className={fieldLabel}>Risk-free rate, %</span>
              <input
                className={inputClass}
                inputMode="decimal"
                value={riskFreePercent}
                onChange={(e) => setRiskFreePercent(e.target.value)}
              />
            </label>
          </div>
        </fieldset>

        <fieldset className="space-y-4">
          <legend className="w-full border-b border-rule pb-2 text-[16px] font-bold">Rules</legend>
          <div className="grid grid-cols-2 gap-3">
            <label className="block space-y-1">
              <span className={fieldLabel}>Max per asset, %</span>
              <input
                className={inputClass}
                inputMode="decimal"
                placeholder="No limit"
                value={maxWeightPercent}
                onChange={(e) => setMaxWeightPercent(e.target.value)}
              />
            </label>
            <label className="block space-y-1">
              <span className={fieldLabel}>Max assets held</span>
              <input
                className={inputClass}
                inputMode="numeric"
                placeholder="No limit"
                value={maxAssets}
                onChange={(e) => setMaxAssets(e.target.value)}
              />
            </label>
          </div>
          <label className="block space-y-1">
            <span className={fieldLabel}>Min expected return, %</span>
            <input
              className={inputClass}
              inputMode="decimal"
              placeholder="No minimum"
              value={minReturnPercent}
              onChange={(e) => setMinReturnPercent(e.target.value)}
            />
          </label>

          {sectorLimits.map((limit) => (
            <div key={limit.id} className="grid grid-cols-[1fr_5.5rem_auto] items-end gap-2">
              <label className="block space-y-1">
                <span className={fieldLabel}>Sector</span>
                <select
                  className={inputClass}
                  value={limit.sector}
                  onChange={(e) => updateSectorLimit(limit.id, { sector: e.target.value })}
                >
                  {sectors.map((sector) => (
                    <option key={sector} value={sector}>
                      {sector}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block space-y-1">
                <span className={fieldLabel}>Max, %</span>
                <input
                  className={inputClass}
                  inputMode="decimal"
                  value={limit.maxPercent}
                  onChange={(e) => updateSectorLimit(limit.id, { maxPercent: e.target.value })}
                />
              </label>
              <button
                type="button"
                onClick={() => removeSectorLimit(limit.id)}
                className="px-2 py-2 text-muted hover:text-loss"
                aria-label={`Remove the limit for ${limit.sector}`}
              >
                ✕
              </button>
            </div>
          ))}
          <button type="button" onClick={addSectorLimit} className="text-[14px] font-semibold text-steel hover:underline">
            Add a sector limit
          </button>
        </fieldset>

        <div className="space-y-3">
          <button type="submit" className={`${primaryButton} w-full`} disabled={optimizing}>
            {optimizing ? "Optimizing…" : "Optimize"}
          </button>
          {error && <p className="text-[14px] text-loss">{error}</p>}
        </div>
      </form>

      <div className="min-w-0 space-y-12">
        {!analysis ? (
          <div className="graph-paper flex min-h-64 items-center justify-center border border-rule p-8 sm:min-h-96 sm:p-10">
            <p className="max-w-sm text-center text-muted">
              Choose the assets and rules on the left, then press Optimize to plot the result on the efficient frontier.
            </p>
          </div>
        ) : (
          <>
            <section className="space-y-6">
              <dl className="flex flex-wrap gap-x-10 gap-y-4 border-b-2 border-ink pb-5">
                {[
                  { label: "Expected return, per year", value: formatPercent(analysis.result.expectedReturn) },
                  { label: "Volatility, per year", value: formatPercent(analysis.result.volatility) },
                  { label: "Sharpe ratio", value: analysis.result.sharpeRatio.toFixed(2) },
                ].map((metric) => (
                  // Term before value for screen readers; shown value-first
                  <div key={metric.label} className="flex flex-col-reverse gap-1.5">
                    <dt className="text-[13px] text-muted">{metric.label}</dt>
                    <dd className="text-[28px] leading-none font-bold tracking-[-0.02em] tabular-nums sm:text-[35px]">
                      {metric.value}
                    </dd>
                  </div>
                ))}
              </dl>

              {!analysis.result.feasible && (
                <div className="border-l-4 border-signal bg-panel py-3 pr-4 pl-4 text-[14px]">
                  <p className="font-semibold">This portfolio breaks some of your rules</p>
                  <ul className="mt-1.5 list-disc space-y-0.5 pl-5 text-muted">
                    {analysis.result.violations.map((violation) => (
                      <li key={violation}>{violation}</li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="graph-paper -mx-4 border-y border-rule p-2 sm:mx-0 sm:border sm:p-6">
                {analysis.frontier ? (
                  <FrontierChart
                    frontier={analysis.frontier}
                    highlight={{
                      volatility: analysis.result.volatility,
                      expectedReturn: analysis.result.expectedReturn,
                      label: "Your portfolio",
                    }}
                  />
                ) : (
                  <p className="py-16 text-center text-muted">
                    The efficient frontier could not be traced: {analysis.frontierError}
                  </p>
                )}
              </div>
            </section>

            <section className="space-y-4">
              <div className="flex flex-wrap items-baseline justify-between gap-2 border-b border-rule pb-2">
                <h2 className="text-[20px] font-bold tracking-[-0.01em]">Weights</h2>
                <span className="text-[13px] text-muted">
                  Based on {analysis.result.observations} trading days, solved in{" "}
                  {analysis.result.solveTimeMillis.toFixed(0)} ms
                </span>
              </div>
              <WeightBars rows={analysis.result.allocations.map((a) => ({ symbol: a.symbol, weight: a.weight }))} />
            </section>

            <section className="space-y-5">
              <div className="border-b border-rule pb-2">
                <h2 className="text-[20px] font-bold tracking-[-0.01em]">Buy whole shares</h2>
              </div>
              <div className="flex flex-wrap items-end gap-3">
                <label className="block w-full space-y-1 sm:w-auto">
                  <span className={fieldLabel}>Budget, USD</span>
                  <input
                    className={`${inputClass} w-full sm:w-44`}
                    inputMode="decimal"
                    value={budget}
                    onChange={(e) => setBudget(e.target.value)}
                  />
                </label>
                <button type="button" className={secondaryButton} onClick={allocate} disabled={allocating}>
                  {allocating ? "Allocating…" : "Allocate"}
                </button>
              </div>
              {allocationError && <p className="text-[14px] text-loss">{allocationError}</p>}

              {allocation && (
                <div className="space-y-6">
                  <table className="w-full text-left text-[14px]">
                    <thead className="border-b-2 border-ink text-[13px] text-muted">
                      <tr>
                        <th className="py-2 pr-3 font-medium">Symbol</th>
                        <th className="py-2 pr-3 text-right font-medium">Shares</th>
                        <th className="hidden py-2 pr-3 text-right font-medium sm:table-cell">Price</th>
                        <th className="py-2 pr-3 text-right font-medium">Amount</th>
                        <th className="py-2 pr-3 text-right font-medium">Target</th>
                        <th className="py-2 text-right font-medium">Actual</th>
                      </tr>
                    </thead>
                    <tbody className="tabular-nums">
                      {allocation.positions
                        .filter((p) => p.shares > 0 || p.targetWeight > 1e-6)
                        .map((p) => (
                          <tr key={p.symbol} className="border-b border-rule">
                            <td className="py-2.5 pr-3 font-semibold tracking-wide">{p.symbol}</td>
                            <td className="py-2.5 pr-3 text-right">{p.shares}</td>
                            <td className="hidden py-2.5 pr-3 text-right text-muted sm:table-cell">
                              {formatMoney(p.price)}
                            </td>
                            <td className="py-2.5 pr-3 text-right">{formatMoney(p.amount)}</td>
                            <td className="py-2.5 pr-3 text-right text-muted">{formatPercent(p.targetWeight)}</td>
                            <td className="py-2.5 text-right font-semibold">{formatPercent(p.actualWeight)}</td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                  <p className="text-[14px] text-muted">
                    {formatMoney(allocation.invested)} invested, {formatMoney(allocation.leftover)} left in cash.{" "}
                    {allocation.optimal
                      ? "No other combination of whole shares gets closer to the target weights."
                      : "This is the closest combination found within the time limit."}
                  </p>

                  <div className="flex flex-wrap items-end gap-3 bg-panel p-4">
                    <label className="block min-w-56 flex-1 space-y-1">
                      <span className={fieldLabel}>Portfolio name</span>
                      <input
                        className={inputClass}
                        placeholder="Balanced growth"
                        value={portfolioName}
                        onChange={(e) => setPortfolioName(e.target.value)}
                      />
                    </label>
                    <button type="button" className={primaryButton} onClick={save} disabled={saving}>
                      {saving ? "Saving…" : "Save portfolio"}
                    </button>
                  </div>
                  {saveError && <p className="text-[14px] text-loss">{saveError}</p>}
                  {saved && (
                    <p className="text-[14px] text-gain">
                      Saved “{saved.name}”.{" "}
                      <Link href={`/portfolios/${saved.id}`} className="font-semibold underline">
                        Open the portfolio
                      </Link>
                    </p>
                  )}
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </div>
  );
}

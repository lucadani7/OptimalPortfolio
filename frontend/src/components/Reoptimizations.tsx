"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { apiPost, apiPut } from "@/lib/client-api";
import { formatDate, formatPercent, formatSignedPercent, formatTimestamp } from "@/lib/format";
import type { Reoptimization, ReoptimizationSchedule } from "@/lib/types";

interface ReoptimizationsProps {
  portfolioId: number;
  schedule: ReoptimizationSchedule | null;
  /** Newest first. */
  history: Reoptimization[];
}

const SCHEDULE_OPTIONS: { value: ReoptimizationSchedule | ""; label: string }[] = [
  { value: "", label: "Off" },
  { value: "WEEKLY", label: "Every Monday" },
  { value: "MONTHLY", label: "On the 1st of each month" },
];

/** Changes smaller than this are not worth a trade, so they are shown muted. */
const NEGLIGIBLE_CHANGE = 0.005;

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

/** Runs the portfolio's strategy again on newer prices, now or on a schedule, and shows how far it has drifted. */
export function Reoptimizations({ portfolioId, schedule, history }: ReoptimizationsProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [saving, startSaving] = useTransition();
  const [checking, startChecking] = useTransition();
  const [latest, ...older] = history;

  function changeSchedule(value: string) {
    setError(null);
    startSaving(async () => {
      try {
        await apiPut(`/api/portfolios/${portfolioId}/schedule`, { frequency: value === "" ? null : value });
        router.refresh();
      } catch (e) {
        setError(errorMessage(e));
      }
    });
  }

  function checkNow() {
    setError(null);
    startChecking(async () => {
      try {
        await apiPost(`/api/portfolios/${portfolioId}/reoptimizations`, {});
        router.refresh();
      } catch (e) {
        setError(errorMessage(e));
      }
    });
  }

  return (
    <section className="space-y-6">
      <h2 className="border-b border-rule pb-2 text-[20px] font-bold tracking-[-0.01em]">Rebalancing</h2>
      <p className="max-w-[40rem] text-[14px] text-muted">
        Runs the same strategy, with the same assets and rules, on the latest prices, and compares its weights
        with what you hold. Nothing is bought or sold.
      </p>

      <div className="flex flex-wrap items-end gap-3">
        <label className="block space-y-1">
          <span className="block text-[13px] text-muted">Check automatically</span>
          <select
            className="w-full rounded-md border border-rule bg-panel px-3 py-2 text-[14px] outline-none focus:border-steel sm:w-60"
            value={schedule ?? ""}
            disabled={saving}
            onChange={(e) => changeSchedule(e.target.value)}
          >
            {SCHEDULE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          onClick={checkNow}
          disabled={checking}
          className="rounded-md border border-ink px-5 py-2 text-[15px] font-semibold hover:bg-panel disabled:opacity-50"
        >
          {checking ? "Checking…" : "Check now"}
        </button>
      </div>
      {error && <p className="text-[14px] text-loss">{error}</p>}

      {!latest ? (
        <p className="text-[14px] text-muted">
          No checks yet. Check now to see the weights the strategy would pick today.
        </p>
      ) : latest.status === "FAILED" ? (
        <div className="border-l-4 border-signal bg-panel py-3 pr-4 pl-4 text-[14px]">
          <p className="font-semibold">The last check found no portfolio that meets the rules</p>
          <p className="mt-1 text-muted">{latest.error}</p>
          <p className="mt-1 text-[13px] text-muted">{formatTimestamp(latest.createdAt)}</p>
        </div>
      ) : (
        <LatestCheck check={latest} />
      )}

      {older.length > 0 && (
        <div className="space-y-2">
          <p className="text-[14px] font-semibold">Earlier checks</p>
          <ul className="divide-y divide-rule border-y border-rule text-[14px]">
            {older.map((check) => (
              <li key={check.id} className="flex flex-wrap justify-between gap-x-6 gap-y-0.5 py-2">
                <span className="text-muted">{formatTimestamp(check.createdAt)}</span>
                {check.status === "SUCCEEDED" && check.turnover != null ? (
                  <span className="tabular-nums">{formatPercent(check.turnover)} to trade</span>
                ) : (
                  <span className="text-loss">No portfolio met the rules</span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function LatestCheck({ check }: { check: Reoptimization }) {
  const turnover = check.turnover ?? 0;
  const changes = [...check.changes].sort(
    (a, b) => Math.abs(b.targetWeight - b.currentWeight) - Math.abs(a.targetWeight - a.currentWeight),
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
        <p className="text-[28px] leading-none font-bold tracking-[-0.02em] tabular-nums sm:text-[35px]">
          {formatPercent(turnover)}
        </p>
        <p className="text-[14px] text-muted">
          of the value would change hands to match the new weights, checked {formatTimestamp(check.createdAt)}
        </p>
      </div>

      <table className="w-full text-left text-[14px]">
        <thead className="border-b-2 border-ink text-[13px] text-muted">
          <tr>
            <th className="py-2 pr-3 font-medium">Asset</th>
            <th className="py-2 pr-3 text-right font-medium">Now</th>
            <th className="py-2 pr-3 text-right font-medium">Target</th>
            <th className="py-2 text-right font-medium">Change</th>
          </tr>
        </thead>
        <tbody className="tabular-nums">
          {changes.map((change) => {
            const difference = change.targetWeight - change.currentWeight;
            const negligible = Math.abs(difference) < NEGLIGIBLE_CHANGE;
            return (
              <tr key={change.symbol} className="border-b border-rule">
                <td className="py-2.5 pr-3 font-semibold tracking-wide">{change.symbol}</td>
                <td className="py-2.5 pr-3 text-right">{formatPercent(change.currentWeight)}</td>
                <td className="py-2.5 pr-3 text-right">{formatPercent(change.targetWeight)}</td>
                <td className={`py-2.5 text-right font-semibold ${negligible ? "font-normal text-muted" : ""}`}>
                  {negligible ? "Hold" : formatSignedPercent(difference)}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      {check.expectedReturn != null && check.volatility != null && check.sharpeRatio != null && (
        <p className="text-[13px] text-muted">
          New weights: {formatPercent(check.expectedReturn)} expected return and{" "}
          {formatPercent(check.volatility)} volatility a year, Sharpe ratio {check.sharpeRatio.toFixed(2)}
          {check.statisticsFrom && check.statisticsTo && (
            <>
              , from prices between {formatDate(check.statisticsFrom)} and {formatDate(check.statisticsTo)}
            </>
          )}
          . Weights are shares of the total value, cash included.
        </p>
      )}
    </div>
  );
}

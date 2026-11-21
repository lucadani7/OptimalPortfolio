"use client";

import {
  CartesianGrid,
  LabelList,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatPercent } from "@/lib/format";
import type { Frontier } from "@/lib/types";

interface FrontierChartProps {
  frontier: Frontier;
  /** The portfolio to mark on the chart, e.g. the result of an optimization. */
  highlight?: { volatility: number; expectedReturn: number; label: string };
  /** Height classes, e.g. "h-80 sm:h-96". */
  className?: string;
}

interface ChartPoint {
  volatility: number;
  expectedReturn: number;
  label?: string;
}

function TooltipContent({ active, payload }: { active?: boolean; payload?: { payload: ChartPoint }[] }) {
  const point = payload?.[0]?.payload;
  if (!active || !point) return null;
  return (
    <div className="rounded-md border border-rule bg-panel px-3 py-2 text-[13px] shadow-sm">
      {point.label && <p className="font-semibold">{point.label}</p>}
      <p className="text-muted">
        Return {formatPercent(point.expectedReturn)}, risk {formatPercent(point.volatility)}
      </p>
    </div>
  );
}

/** Risk (x) against return (y): the efficient frontier, every asset, and an optional marked portfolio. */
export function FrontierChart({ frontier, highlight, className = "h-80 sm:h-96" }: FrontierChartProps) {
  const curve: ChartPoint[] = frontier.points.map((point) => ({
    volatility: point.volatility,
    expectedReturn: point.expectedReturn,
  }));
  const assets: ChartPoint[] = frontier.assets.map((asset) => ({
    volatility: asset.volatility,
    expectedReturn: asset.expectedReturn,
    label: asset.symbol,
  }));

  return (
    <div className={`w-full ${className}`}>
      <ResponsiveContainer width="100%" height="100%">
        <ScatterChart margin={{ top: 20, right: 28, bottom: 24, left: 8 }}>
          <CartesianGrid stroke="var(--rule)" strokeOpacity={0.5} />
          <XAxis
            type="number"
            dataKey="volatility"
            domain={[0, "auto"]}
            tickFormatter={(value: number) => formatPercent(value)}
            tick={{ fontSize: 12, fill: "var(--muted)" }}
            axisLine={{ stroke: "var(--ink)", strokeOpacity: 0.5 }}
            tickLine={false}
            label={{ value: "Risk (annual volatility)", position: "insideBottom", offset: -16, fontSize: 12, fill: "var(--muted)" }}
          />
          <YAxis
            type="number"
            dataKey="expectedReturn"
            tickFormatter={(value: number) => formatPercent(value)}
            tick={{ fontSize: 12, fill: "var(--muted)" }}
            width={64}
            axisLine={{ stroke: "var(--ink)", strokeOpacity: 0.5 }}
            tickLine={false}
            label={{ value: "Expected return", angle: -90, position: "insideLeft", offset: 4, fontSize: 12, fill: "var(--muted)" }}
          />
          <Tooltip content={<TooltipContent />} cursor={false} />
          <Scatter
            data={curve}
            fill="var(--steel)"
            line={{ stroke: "var(--steel)", strokeWidth: 2.5 }}
            lineType="joint"
            shape={() => <g />}
            isAnimationActive={false}
          />
          <Scatter data={assets} fill="var(--muted)" isAnimationActive={false}>
            <LabelList dataKey="label" position="right" offset={8} fontSize={12} fill="var(--ink)" />
          </Scatter>
          {highlight && (
            <Scatter
              data={[highlight]}
              fill="var(--signal)"
              shape="diamond"
              legendType="none"
              isAnimationActive={false}
            >
              <LabelList dataKey="label" position="left" offset={10} fontSize={12} fontWeight={700} fill="var(--signal)" />
            </Scatter>
          )}
        </ScatterChart>
      </ResponsiveContainer>
    </div>
  );
}

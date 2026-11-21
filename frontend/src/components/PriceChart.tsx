"use client";

import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { formatDate, formatMoney } from "@/lib/format";
import type { PricePoint } from "@/lib/types";

interface PriceChartProps {
  prices: PricePoint[];
  currency: string;
}

export function PriceChart({ prices, currency }: PriceChartProps) {
  if (prices.length === 0) {
    return <p className="py-24 text-center text-muted">There are no prices in this period. Choose a longer one.</p>;
  }

  return (
    <div className="h-64 w-full sm:h-80">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={prices} margin={{ top: 12, right: 12, bottom: 4, left: 4 }}>
          <CartesianGrid stroke="var(--rule)" strokeOpacity={0.6} vertical={false} />
          <XAxis
            dataKey="date"
            tickFormatter={(date: string) => formatDate(date)}
            minTickGap={56}
            tick={{ fontSize: 12, fill: "var(--muted)" }}
            axisLine={{ stroke: "var(--rule)" }}
            tickLine={false}
          />
          <YAxis
            domain={["auto", "auto"]}
            tickFormatter={(value: number) => formatMoney(value, currency)}
            width={84}
            tick={{ fontSize: 12, fill: "var(--muted)" }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip
            labelFormatter={(date) => formatDate(String(date))}
            formatter={(value) => [formatMoney(Number(value), currency), "Close"]}
            contentStyle={{
              background: "var(--panel)",
              border: "1px solid var(--rule)",
              borderRadius: 6,
              fontSize: 13,
              color: "var(--ink)",
            }}
          />
          <Line
            type="monotone"
            dataKey="close"
            stroke="var(--steel)"
            strokeWidth={1.75}
            dot={false}
            activeDot={{ r: 4, fill: "var(--signal)", stroke: "var(--panel)", strokeWidth: 2 }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

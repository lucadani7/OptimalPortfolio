"use client";

import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { formatDate, formatMoney } from "@/lib/format";
import type { PricePoint } from "@/lib/types";

interface PriceChartProps {
  prices: PricePoint[];
  currency: string;
}

export function PriceChart({ prices, currency }: PriceChartProps) {
  if (prices.length === 0) {
    return <p className="py-16 text-center text-sm text-zinc-500">No prices in this period.</p>;
  }

  const rising = prices[prices.length - 1].close >= prices[0].close;
  const color = rising ? "#059669" : "#dc2626";

  return (
    <div className="h-80 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={prices} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id="priceFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={color} stopOpacity={0.25} />
              <stop offset="100%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="currentColor" strokeOpacity={0.1} vertical={false} />
          <XAxis
            dataKey="date"
            tickFormatter={(date: string) => formatDate(date)}
            minTickGap={48}
            tick={{ fontSize: 12 }}
            stroke="currentColor"
            strokeOpacity={0.4}
          />
          <YAxis
            domain={["auto", "auto"]}
            tickFormatter={(value: number) => formatMoney(value, currency)}
            width={88}
            tick={{ fontSize: 12 }}
            stroke="currentColor"
            strokeOpacity={0.4}
          />
          <Tooltip
            labelFormatter={(date) => formatDate(String(date))}
            formatter={(value) => [formatMoney(Number(value), currency), "Close"]}
            contentStyle={{ borderRadius: 8, fontSize: 13 }}
          />
          <Area
            type="monotone"
            dataKey="close"
            stroke={color}
            strokeWidth={2}
            fill="url(#priceFill)"
            dot={false}
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

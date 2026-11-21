import type { Metadata } from "next";
import { Optimizer } from "@/components/Optimizer";
import { apiGet } from "@/lib/api";
import type { Asset, Strategy } from "@/lib/types";

export const metadata: Metadata = { title: "Optimize" };

export default async function OptimizePage() {
  const [assets, strategies] = await Promise.all([
    apiGet<Asset[]>("/api/assets"),
    apiGet<Strategy[]>("/api/optimization/strategies"),
  ]);

  return (
    <div className="space-y-10">
      <div className="max-w-[40rem] space-y-3">
        <h1 className="text-[28px] leading-tight font-extrabold tracking-[-0.025em] sm:text-[35px]">Optimize a portfolio</h1>
        <p className="text-[16px] text-muted sm:text-[17px]">
          Pick the assets and the rules, see where the result sits on the efficient frontier, then turn it into
          whole shares for your budget.
        </p>
      </div>
      <Optimizer assets={assets} strategies={strategies} />
    </div>
  );
}

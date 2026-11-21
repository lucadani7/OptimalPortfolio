import type { components } from "./api-schema";

type Schemas = components["schemas"];

/*
 * The generated schema marks every response field as optional. These helpers make the fields
 * that the backend always sends required, so components don't need null checks for them.
 */

/** Makes the listed keys required and non-null. */
type WithRequired<T, K extends keyof T> = T & { [P in K]-?: NonNullable<T[P]> };

/** Makes every key required and non-null, for responses where the backend always sends every field. */
type Strict<T> = { [K in keyof T]-?: NonNullable<T[K]> };

export type Asset = WithRequired<
  Schemas["AssetResponse"],
  "id" | "type" | "symbol" | "name" | "sector" | "currency" | "createdAt" | "updatedAt"
>;

export type AssetType = Asset["type"];

export type PricePoint = Strict<Schemas["PricePointResponse"]>;

export type Strategy = Strict<Schemas["StrategyResponse"]>;

/** One constraint of an optimization request; the "type" property selects the kind. */
export type ConstraintRequest = NonNullable<Schemas["OptimizationRequest"]["constraints"]>[number];

export type OptimizationRequest = Schemas["OptimizationRequest"];
export type FrontierRequest = Schemas["FrontierRequest"];
export type AllocationRequest = Schemas["AllocationRequest"];
export type CreatePortfolioRequest = Schemas["CreatePortfolioRequest"];

export type Allocation = Strict<Schemas["Allocation"]>;

export type OptimizationResult = Omit<Strict<Schemas["OptimizationResponse"]>, "allocations"> & {
  allocations: Allocation[];
};

export type Frontier = Omit<Strict<Schemas["FrontierResponse"]>, "assets" | "points"> & {
  assets: Strict<Schemas["AssetPoint"]>[];
  points: Strict<Schemas["Point"]>[];
};

export type Position = Strict<Schemas["Position"]>;

export type AllocationResult = Omit<Strict<Schemas["AllocationResponse"]>, "positions"> & {
  positions: Position[];
};

export type PortfolioSummary = Strict<Schemas["PortfolioSummaryResponse"]>;

/** current* fields stay optional: they are missing when no price is known for the asset. */
export type Holding = WithRequired<
  Schemas["HoldingResponse"],
  "symbol" | "name" | "shares" | "purchasePrice" | "priceDate" | "targetWeight" | "cost"
>;

export type Portfolio = Omit<Strict<Schemas["PortfolioResponse"]>, "holdings" | "valuation" | "constraints"> & {
  constraints: ConstraintRequest[];
  holdings: Holding[];
  valuation: Strict<Schemas["Valuation"]>;
};

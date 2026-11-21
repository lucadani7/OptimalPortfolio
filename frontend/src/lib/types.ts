import type { components } from "./api-schema";

type Schemas = components["schemas"];

/**
 * The generated schema marks every response field as optional. These helpers make
 * the fields that the backend always sends required, so pages don't need null checks for them.
 */
type WithRequired<T, K extends keyof T> = T & { [P in K]-?: NonNullable<T[P]> };

export type Asset = WithRequired<
  Schemas["AssetResponse"],
  "id" | "type" | "symbol" | "name" | "sector" | "currency" | "createdAt" | "updatedAt"
>;

export type AssetType = Asset["type"];

export type PricePoint = Required<Schemas["PricePointResponse"]>;

import { formatPercent } from "./format";
import type { ConstraintRequest } from "./types";

/** A constraint in plain words, e.g. "At most 30.00% in Technology". */
export function describeConstraint(constraint: ConstraintRequest): string {
  switch (constraint.type) {
    case "WEIGHT_BOUNDS":
      return constraint.min > 0
        ? `Each asset between ${formatPercent(constraint.min)} and ${formatPercent(constraint.max)}`
        : `At most ${formatPercent(constraint.max)} in any single asset`;
    case "ASSET_WEIGHT_BOUNDS":
      return `${constraint.symbol} between ${formatPercent(constraint.min)} and ${formatPercent(constraint.max)}`;
    case "SECTOR_LIMIT":
      return `At most ${formatPercent(constraint.maxWeight)} in ${constraint.sector}`;
    case "MAX_ASSETS":
      return `At most ${constraint.count} ${constraint.count === 1 ? "asset" : "assets"}`;
    case "MIN_EXPECTED_RETURN":
      return `Expected return of at least ${formatPercent(constraint.value)} a year`;
  }
}

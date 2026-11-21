/** Plain-language names for the strategies the backend offers. */
const STRATEGY_LABELS: Record<string, string> = {
  "max-sharpe": "Best return for the risk",
  "min-variance": "Lowest risk",
  "equal-weight": "Equal weights",
};

export function strategyLabel(name: string): string {
  return STRATEGY_LABELS[name] ?? name;
}

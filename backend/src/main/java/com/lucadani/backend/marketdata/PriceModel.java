package com.lucadani.backend.marketdata;

/**
 * Daily price dynamics used to simulate synthetic price history.
 * Given today's price and a standard normal shock, returns tomorrow's price.
 */
@FunctionalInterface
public interface PriceModel {

    double TRADING_DAYS_PER_YEAR = 252.0;
    double DT = 1.0 / TRADING_DAYS_PER_YEAR;

    double next(double price, double shock);

    /**
     * Simulates a full price path. The first price is {@code startPrice};
     * each following price is obtained by applying one shock.
     *
     * @return an array of {@code shocks.length + 1} prices
     */
    default double[] path(double startPrice, double[] shocks) {
        double[] prices = new double[shocks.length + 1];
        prices[0] = startPrice;
        for (int i = 0; i < shocks.length; i++) {
            prices[i + 1] = next(prices[i], shocks[i]);
        }
        return prices;
    }

    /**
     * Geometric Brownian motion: prices drift exponentially, with volatility proportional to the price.
     * Suitable for stocks and equity ETFs.
     *
     * @param annualDrift      expected annual return (0.08 = 8%)
     * @param annualVolatility annual volatility (0.20 = 20%)
     */
    static PriceModel geometricBrownian(double annualDrift, double annualVolatility) {
        double drift = (annualDrift - 0.5 * annualVolatility * annualVolatility) * DT;
        double diffusion = annualVolatility * Math.sqrt(DT);
        return (price, shock) -> price * Math.exp(drift + diffusion * shock);
    }

    /**
     * Exponential Ornstein-Uhlenbeck process: the log-price is pulled back towards a long-term level.
     * Prices oscillate around that level and always stay positive. Suitable for bonds.
     *
     * @param longTermPrice    level the price reverts to
     * @param reversionSpeed   annual speed of reversion (higher = faster return to the level)
     * @param annualVolatility annual volatility of the log-price
     */
    static PriceModel meanReverting(double longTermPrice, double reversionSpeed, double annualVolatility) {
        double longTermLog = Math.log(longTermPrice);
        double pull = reversionSpeed * DT;
        double diffusion = annualVolatility * Math.sqrt(DT);
        return (price, shock) -> {
            double logPrice = Math.log(price);
            return Math.exp(logPrice + pull * (longTermLog - logPrice) + diffusion * shock);
        };
    }
}

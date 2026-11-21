package com.lucadani.backend.optimization;

import com.lucadani.backend.optimization.PortfolioConstraint.SectorLimit;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Turns target weights into whole numbers of shares for a given cash budget, using constraint programming.
 * <p>
 * Amounts are handled in cents, so the problem is purely integer. For every asset the solver chooses a
 * number of shares; the objective is the total distance, in cents, between the money put into each asset
 * and its target amount, plus the cash left uninvested. The budget can never be exceeded, assets with
 * a zero target weight are never bought, and, when the allocation is made for an {@link OptimizationProblem},
 * the maximum weight of every asset and every sector limit are respected as caps in money.
 * Minimum weights and the minimum expected return are not enforced: with whole shares they may be unreachable.
 * <p>
 * A greedy rounding gives the solver a good first solution; the search then improves it until it is
 * proven optimal or the time limit is reached, in which case the best allocation found is returned.
 */
public final class DiscreteAllocator {

    /** Largest budget that keeps every amount in cents within {@code int} range. */
    public static final double MAX_BUDGET = 10_000_000;

    private static final Duration DEFAULT_TIME_LIMIT = Duration.ofSeconds(2);

    private final Duration timeLimit;

    public DiscreteAllocator() {
        this(DEFAULT_TIME_LIMIT);
    }

    public DiscreteAllocator(Duration timeLimit) {
        if (timeLimit.isNegative() || timeLimit.isZero()) {
            throw new IllegalArgumentException("timeLimit must be positive");
        }
        this.timeLimit = timeLimit;
    }

    /** Allocation without caps: only the budget limits the purchases. */
    public DiscreteAllocation allocate(List<String> symbols, double[] targetWeights, double[] prices, double budget) {
        double[] noCaps = new double[symbols.size()];
        Arrays.fill(noCaps, 1.0);
        return allocate(symbols, targetWeights, prices, budget, noCaps, List.of());
    }

    /** Allocation that keeps the per-asset maximum weights and the sector limits of the problem. */
    public DiscreteAllocation allocate(OptimizationProblem problem, double[] targetWeights, double[] prices, double budget) {
        List<SectorCap> sectorCaps = new ArrayList<>();
        for (SectorLimit limit : problem.constraintsOfType(SectorLimit.class)) {
            List<Integer> members = new ArrayList<>();
            for (int i = 0; i < problem.assetCount(); i++) {
                if (problem.sector(i).equals(limit.sector())) {
                    members.add(i);
                }
            }
            sectorCaps.add(new SectorCap(members.stream().mapToInt(Integer::intValue).toArray(), limit.maxWeight()));
        }
        return allocate(problem.statistics().symbols(), targetWeights, prices, budget,
                problem.upperBounds(), sectorCaps);
    }

    /** The assets of one sector and the maximum fraction of the budget they may receive together. */
    private record SectorCap(int[] members, double maxWeight) {
    }

    private DiscreteAllocation allocate(List<String> symbols, double[] targetWeights, double[] prices, double budget,
                                        double[] maxWeights, List<SectorCap> sectorCaps) {
        int n = symbols.size();
        validate(n, targetWeights, prices, budget);
        long start = System.nanoTime();

        int budgetCents = (int) Math.floor(budget * 100);
        int[] priceCents = new int[n];
        int[] targetCents = new int[n];
        int[] capCents = new int[n];
        int[] maxShares = new int[n];
        for (int i = 0; i < n; i++) {
            priceCents[i] = Math.max(1, (int) Math.round(prices[i] * 100));
            targetCents[i] = (int) Math.round(targetWeights[i] * budgetCents);
            capCents[i] = (int) Math.floor(Math.min(1.0, maxWeights[i]) * budgetCents);
            maxShares[i] = targetWeights[i] > PortfolioEvaluation.HOLDING_THRESHOLD ? capCents[i] / priceCents[i] : 0;
        }
        int[][] sectorMembers = new int[sectorCaps.size()][];
        int[] sectorCapCents = new int[sectorCaps.size()];
        for (int k = 0; k < sectorCaps.size(); k++) {
            sectorMembers[k] = sectorCaps.get(k).members();
            sectorCapCents[k] = (int) Math.floor(sectorCaps.get(k).maxWeight() * budgetCents);
        }

        Model model = new Model("discrete-allocation");
        IntVar[] shares = new IntVar[n];
        IntVar[] amounts = new IntVar[n];
        IntVar[] deviations = new IntVar[n];
        for (int i = 0; i < n; i++) {
            shares[i] = model.intVar("shares_" + symbols.get(i), 0, maxShares[i]);
            amounts[i] = model.intVar("amount_" + symbols.get(i), 0, maxShares[i] * priceCents[i]);
            IntVar difference = model.intVar("difference_" + symbols.get(i),
                    -targetCents[i], maxShares[i] * priceCents[i] - targetCents[i]);
            deviations[i] = model.intVar("deviation_" + symbols.get(i), 0, Math.max(targetCents[i], budgetCents));

            // amount_i = price_i * shares_i
            model.scalar(new IntVar[]{shares[i]}, new int[]{priceCents[i]}, "=", amounts[i]).post();
            // deviation_i = |amount_i - target_i|
            model.arithm(amounts[i], "-", difference, "=", targetCents[i]).post();
            model.absolute(deviations[i], difference).post();
        }
        IntVar invested = model.intVar("invested", 0, budgetCents);
        model.sum(amounts, "=", invested).post();
        for (int k = 0; k < sectorMembers.length; k++) {
            IntVar[] members = Arrays.stream(sectorMembers[k]).mapToObj(i -> amounts[i]).toArray(IntVar[]::new);
            if (members.length > 0) {
                model.sum(members, "<=", sectorCapCents[k]).post();
            }
        }

        // objective = sum(deviation_i) + (budget - invested)
        IntVar objective = model.intVar("objective", 0, (n + 1) * budgetCents);
        IntVar[] terms = new IntVar[n + 2];
        int[] coefficients = new int[n + 2];
        for (int i = 0; i < n; i++) {
            terms[i] = deviations[i];
            coefficients[i] = 1;
        }
        terms[n] = invested;
        coefficients[n] = -1;
        terms[n + 1] = objective;
        coefficients[n + 1] = -1;
        model.scalar(terms, coefficients, "=", -budgetCents).post();

        Solver solver = model.getSolver();
        solver.setSearch(Search.inputOrderUBSearch(shares));
        solver.limitTime(timeLimit.toMillis() + "ms");
        int[] greedy = greedyShares(priceCents, targetCents, budgetCents, capCents, sectorMembers, sectorCapCents);
        for (int i = 0; i < n; i++) {
            solver.addHint(shares[i], greedy[i]);
        }
        model.setObjective(Model.MINIMIZE, objective);

        Solution best = new Solution(model, shares);
        while (solver.solve()) {
            best.record();
        }
        boolean optimal = !solver.isStopCriterionMet();

        int[] chosen = new int[n];
        if (solver.getSolutionCount() > 0) {
            for (int i = 0; i < n; i++) {
                chosen[i] = best.getIntVal(shares[i]);
            }
        } else {
            // Not even one solution in time: fall back to the greedy rounding, which is always within budget
            chosen = greedy;
            optimal = false;
        }
        return result(symbols, chosen, prices, targetWeights, budget, optimal,
                Duration.ofNanos(System.nanoTime() - start));
    }

    /** Greedy rounding without caps. */
    static int[] greedyShares(int[] priceCents, int[] targetCents, int budgetCents) {
        int[] noCaps = new int[priceCents.length];
        Arrays.fill(noCaps, budgetCents);
        return greedyShares(priceCents, targetCents, budgetCents, noCaps, new int[0][], new int[0]);
    }

    /**
     * Buys the floor of every target, then spends the remaining cash one share at a time on the asset
     * furthest below its target, as long as the share fits in the budget, the asset cap and the sector caps.
     */
    static int[] greedyShares(int[] priceCents, int[] targetCents, int budgetCents,
                              int[] capCents, int[][] sectorMembers, int[] sectorCapCents) {
        int n = priceCents.length;
        int[] shares = new int[n];
        for (int i = 0; i < n; i++) {
            shares[i] = Math.min(targetCents[i], capCents[i]) / priceCents[i];
        }
        // The floors respect the asset caps, but a sector may still be over its cap: sell until it fits
        for (int k = 0; k < sectorMembers.length; k++) {
            while (sectorAmount(shares, priceCents, sectorMembers[k]) > sectorCapCents[k]) {
                int largest = -1;
                for (int i : sectorMembers[k]) {
                    if (shares[i] > 0 && (largest < 0 || shares[i] * (long) priceCents[i] > shares[largest] * (long) priceCents[largest])) {
                        largest = i;
                    }
                }
                shares[largest]--;
            }
        }
        long spent = 0;
        for (int i = 0; i < n; i++) {
            spent += (long) shares[i] * priceCents[i];
        }
        while (true) {
            int best = -1;
            long bestGap = 0;
            for (int i = 0; i < n; i++) {
                long amount = (long) shares[i] * priceCents[i];
                long gap = targetCents[i] - amount;
                boolean fits = spent + priceCents[i] <= budgetCents
                        && amount + priceCents[i] <= capCents[i]
                        && fitsSectors(i, shares, priceCents, sectorMembers, sectorCapCents);
                if (targetCents[i] > 0 && fits && gap > bestGap) {
                    best = i;
                    bestGap = gap;
                }
            }
            if (best < 0) {
                return shares;
            }
            shares[best]++;
            spent += priceCents[best];
        }
    }

    private static boolean fitsSectors(int asset, int[] shares, int[] priceCents,
                                       int[][] sectorMembers, int[] sectorCapCents) {
        for (int k = 0; k < sectorMembers.length; k++) {
            for (int member : sectorMembers[k]) {
                if (member == asset
                        && sectorAmount(shares, priceCents, sectorMembers[k]) + priceCents[asset] > sectorCapCents[k]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static long sectorAmount(int[] shares, int[] priceCents, int[] members) {
        long total = 0;
        for (int i : members) {
            total += (long) shares[i] * priceCents[i];
        }
        return total;
    }

    private static DiscreteAllocation result(List<String> symbols, int[] shares, double[] prices,
                                             double[] targetWeights, double budget, boolean optimal,
                                             Duration solveTime) {
        int n = shares.length;
        double invested = 0;
        double[] actualWeights = new double[n];
        for (int i = 0; i < n; i++) {
            double amount = shares[i] * Math.round(prices[i] * 100) / 100.0;
            invested += amount;
            actualWeights[i] = amount / budget;
        }
        return new DiscreteAllocation(symbols, shares, prices, targetWeights, actualWeights,
                budget, invested, budget - invested, optimal, solveTime);
    }

    private static void validate(int n, double[] targetWeights, double[] prices, double budget) {
        if (n == 0) {
            throw new IllegalArgumentException("At least one asset is required");
        }
        if (targetWeights.length != n || prices.length != n) {
            throw new IllegalArgumentException("Expected " + n + " weights and prices");
        }
        if (!(budget > 0 && budget <= MAX_BUDGET)) {
            throw new IllegalArgumentException("budget must be positive and at most " + (long) MAX_BUDGET);
        }
        for (int i = 0; i < n; i++) {
            if (!(prices[i] > 0)) {
                throw new IllegalArgumentException("Prices must be positive");
            }
            if (!(targetWeights[i] >= 0)) {
                throw new IllegalArgumentException("Target weights must not be negative");
            }
        }
    }
}

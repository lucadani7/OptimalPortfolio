package com.lucadani.backend.optimization;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.exception.NoFeasibleSolutionException;
import com.lucadani.backend.optimization.PortfolioConstraint.MaxAssets;
import com.lucadani.backend.optimization.PortfolioConstraint.MinExpectedReturn;
import com.lucadani.backend.optimization.PortfolioConstraint.SectorLimit;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import java.util.List;

/**
 * Translates an {@link OptimizationProblem} into an ojAlgo model.
 * <p>
 * Variables are the asset weights. Every constraint of the problem becomes a linear constraint;
 * {@link MaxAssets} adds one binary "held" variable per asset, which turns the problem into a
 * mixed-integer program. A new instance is needed for every solve, because ojAlgo models are stateful.
 */
final class MeanVarianceModel {

    private final OptimizationProblem problem;
    private final ExpressionsBasedModel model = new ExpressionsBasedModel();
    private final Variable[] weights;

    /** Model of the problem as it is. */
    static MeanVarianceModel of(OptimizationProblem problem) {
        return new MeanVarianceModel(problem, Double.NEGATIVE_INFINITY);
    }

    /** Model of the problem with an additional lower bound on the expected return, used to trace the frontier. */
    static MeanVarianceModel withMinReturn(OptimizationProblem problem, double minReturn) {
        return new MeanVarianceModel(problem, minReturn);
    }

    /**
     * @param extraMinReturn additional lower bound on the expected return;
     *                       {@link Double#NEGATIVE_INFINITY} for none
     */
    private MeanVarianceModel(OptimizationProblem problem, double extraMinReturn) {
        this.problem = problem;
        MarketStatistics stats = problem.statistics();
        int n = problem.assetCount();
        double[] lower = problem.lowerBounds();
        double[] upper = problem.upperBounds();

        weights = new Variable[n];
        for (int i = 0; i < n; i++) {
            weights[i] = model.newRealVariable("w_" + problem.symbol(i)).lower(lower[i]).upper(upper[i]);
        }

        Expression budget = model.newConstraint("budget").level(1);
        for (Variable weight : weights) {
            budget.set(weight, 1);
        }

        List<SectorLimit> sectorLimits = problem.constraintsOfType(SectorLimit.class);
        for (int k = 0; k < sectorLimits.size(); k++) {
            SectorLimit limit = sectorLimits.get(k);
            Expression sector = model.newConstraint("sector_" + k).upper(limit.maxWeight());
            for (int i = 0; i < n; i++) {
                if (problem.sector(i).equals(limit.sector())) {
                    sector.set(weights[i], 1);
                }
            }
        }

        double minReturn = problem.constraintsOfType(MinExpectedReturn.class).stream()
                .mapToDouble(MinExpectedReturn::value)
                .reduce(extraMinReturn, Math::max);
        if (minReturn > Double.NEGATIVE_INFINITY) {
            Expression expectedReturn = model.newConstraint("min_return").lower(minReturn);
            for (int i = 0; i < n; i++) {
                expectedReturn.set(weights[i], stats.meanReturn(i));
            }
        }

        problem.constraintsOfType(MaxAssets.class).stream()
                .mapToInt(MaxAssets::count)
                .min()
                .ifPresent(maxAssets -> addCardinality(maxAssets, upper));
    }

    /** held_i is 1 when asset I may have a non-zero weight; at most maxAssets of them can be 1. */
    private void addCardinality(int maxAssets, double[] upper) {
        Expression cardinality = model.newConstraint("cardinality").upper(maxAssets);
        for (int i = 0; i < weights.length; i++) {
            Variable held = model.newBinaryVariable("held_" + problem.symbol(i));
            // weight_i <= upper_i * held_i
            model.newConstraint("link_" + problem.symbol(i)).upper(0)
                    .set(weights[i], 1)
                    .set(held, -upper[i]);
            cardinality.set(held, 1);
        }
    }

    /** Weights with the lowest variance among those satisfying every constraint. */
    double[] minimiseVariance() {
        MarketStatistics stats = problem.statistics();
        Expression objective = model.getObjective();
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights.length; j++) {
                objective.set(weights[i], weights[j], stats.covariance(i, j));
            }
        }
        return solve(model.minimise());
    }

    /** Weights with the highest expected return among those satisfying every constraint. */
    double[] maximiseReturn() {
        MarketStatistics stats = problem.statistics();
        Expression objective = model.getObjective();
        for (int i = 0; i < weights.length; i++) {
            objective.set(weights[i], stats.meanReturn(i));
        }
        return solve(model.maximise());
    }

    private double[] solve(Optimisation.Result result) {
        if (!result.getState().isFeasible()) {
            throw new NoFeasibleSolutionException("No portfolio satisfies all the constraints (solver state: "
                    + result.getState() + ")");
        }
        double[] lower = problem.lowerBounds();
        double[] upper = problem.upperBounds();
        double[] values = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            // Remove tiny numerical noise such as -1e-12 by clamping to the bounds
            values[i] = Math.clamp(upper[i], lower[i], result.doubleValue(i));
        }
        return values;
    }
}

package com.lucadani.backend.service;

import com.lucadani.backend.optimization.FrontierPoint;
import com.lucadani.backend.optimization.OptimizationProblem;

import java.util.List;

/** An efficient frontier together with the problem it was traced for. */
public record FrontierRun(OptimizationProblem problem, List<FrontierPoint> points) {

    public FrontierRun {
        points = List.copyOf(points);
    }
}

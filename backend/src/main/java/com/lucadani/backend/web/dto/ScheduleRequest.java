package com.lucadani.backend.web.dto;

import com.lucadani.backend.model.ReoptimizationSchedule;
import org.jspecify.annotations.Nullable;

/** How often to reoptimize a portfolio on its own; a null frequency stops it. */
public record ScheduleRequest(@Nullable ReoptimizationSchedule frequency) {
}

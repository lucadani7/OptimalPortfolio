package com.lucadani.backend.model;

/** How often a portfolio is reoptimized on its own, in the morning, with the prices of the last close. */
public enum ReoptimizationSchedule {
    /** Mondays at 07:00. */
    WEEKLY("0 7 * * MON"),
    /** The first day of every month at 07:00. */
    MONTHLY("0 7 1 * *");

    private final String cron;

    ReoptimizationSchedule(String cron) {
        this.cron = cron;
    }

    public String cron() {
        return cron;
    }
}

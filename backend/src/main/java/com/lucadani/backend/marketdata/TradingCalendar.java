package com.lucadani.backend.marketdata;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Trading days, approximated as weekdays. Exchange holidays are not taken into account. */
public final class TradingCalendar {

    private TradingCalendar() {
    }

    public static boolean isTradingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /** Trading days from {@code start} to {@code end}, both included; empty when start is after end. */
    public static List<LocalDate> tradingDays(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            return List.of();
        }
        return start.datesUntil(end.plusDays(1)).filter(TradingCalendar::isTradingDay).toList();
    }
}

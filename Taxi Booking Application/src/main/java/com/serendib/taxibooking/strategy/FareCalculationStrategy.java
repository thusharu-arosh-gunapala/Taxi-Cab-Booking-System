package com.serendib.taxibooking.strategy;

import com.serendib.taxibooking.model.FareRule;

public interface FareCalculationStrategy {
    double calculate(double distanceKilometres, FareRule rule, boolean peakHour);
}

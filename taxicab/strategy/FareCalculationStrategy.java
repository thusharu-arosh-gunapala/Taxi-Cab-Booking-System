package com.taxicab.strategy;

import com.taxicab.model.FareRule;

public interface FareCalculationStrategy {
    double calculate(double distanceKilometres, FareRule rule, boolean peakHour);
}

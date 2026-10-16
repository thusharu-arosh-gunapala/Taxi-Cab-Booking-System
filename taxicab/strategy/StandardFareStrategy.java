package com.taxicab.strategy;

import com.taxicab.model.FareRule;
import org.springframework.stereotype.Component;

@Component
public class StandardFareStrategy implements FareCalculationStrategy {

    @Override
    public double calculate(double distanceKilometres, FareRule rule, boolean peakHour) {
        if (rule == null) {
            throw new IllegalArgumentException("Fare rule is required");
        }
        double multiplier = peakHour && rule.getPeakHourMultiplier() > 1.0 ? rule.getPeakHourMultiplier() : 1.0;
        double distanceCharge = distanceKilometres * rule.getPerKilometreRate() * multiplier;
        double baseCharge = rule.getBaseFare();
        double total = baseCharge + distanceCharge + rule.getServiceCharge();
        return Math.max(total, rule.getMinimumFare());
    }
}

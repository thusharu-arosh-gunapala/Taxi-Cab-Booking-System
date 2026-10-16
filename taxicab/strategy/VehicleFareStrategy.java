package com.taxicab.strategy;

import com.taxicab.model.FareRule;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class VehicleFareStrategy implements FareCalculationStrategy {

    @Override
    public double calculate(double distanceKilometres, FareRule rule, boolean peakHour) {
        if (rule == null) {
            throw new IllegalArgumentException("Fare rule is required");
        }
        double multiplier = peakHour ? rule.getPeakHourMultiplier() : 1.0;
        double distanceCharge = distanceKilometres * rule.getPerKilometreRate() * multiplier;
        double adjustedBaseFare = rule.getBaseFare() * multiplier;
        double total = adjustedBaseFare + distanceCharge + rule.getServiceCharge();
        return Math.max(total, rule.getMinimumFare());
    }
}

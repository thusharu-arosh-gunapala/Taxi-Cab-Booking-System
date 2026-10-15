package com.taxicab.service;

import com.taxicab.exception.RouteCalculationException;
import org.springframework.stereotype.Service;

@Service
public class DistanceService {

    public double metresToKilometres(double metres) {
        if (metres < 0) {
            throw new RouteCalculationException("Route distance cannot be negative.");
        }
        return metres / 1000.0;
    }

    public double validateDistance(double metres) {
        if (metres <= 0) {
            throw new RouteCalculationException("Road distance must be greater than zero.");
        }
        return metres;
    }
}

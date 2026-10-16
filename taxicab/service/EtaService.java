package com.taxicab.service;

import com.taxicab.exception.RouteCalculationException;
import org.springframework.stereotype.Service;

@Service
public class EtaService {

    public double secondsToMinutes(double seconds) {
        if (seconds < 0) {
            throw new RouteCalculationException("Duration cannot be negative.");
        }
        return seconds / 60.0;
    }
}

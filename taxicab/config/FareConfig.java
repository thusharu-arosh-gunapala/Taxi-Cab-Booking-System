package com.taxicab.config;

import com.taxicab.model.FareRule;
import com.taxicab.model.VehicleCategory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.fare")
public class FareConfig {

    private String currency = "LKR";
    private final Map<String, FareRule> rules = new HashMap<>();

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, FareRule> getRules() {
        return rules;
    }

    public void setRules(Map<String, FareRule> rules) {
        this.rules.clear();
        this.rules.putAll(rules);
    }

    public FareRule getRule(VehicleCategory category) {
        String key = category.name();
        return rules.getOrDefault(key, defaultRule(category));
    }

    private FareRule defaultRule(VehicleCategory category) {
        return switch (category) {
            case TUK_TUK -> new FareRule(category, 350.0, 80.0, 350.0, 0.0, 1.0, currency);
            case MINI -> new FareRule(category, 450.0, 95.0, 450.0, 0.0, 1.0, currency);
            case CAR -> new FareRule(category, 550.0, 110.0, 550.0, 0.0, 1.0, currency);
            case VAN -> new FareRule(category, 700.0, 150.0, 700.0, 0.0, 1.0, currency);
            case PREMIUM -> new FareRule(category, 900.0, 220.0, 900.0, 0.0, 1.05, currency);
        };
    }
}

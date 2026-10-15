package com.taxicab.model;

public class FareRule {
    private VehicleCategory vehicleCategory;
    private double baseFare;
    private double perKilometreRate;
    private double minimumFare;
    private double serviceCharge;
    private double peakHourMultiplier;
    private String currency;

    public FareRule() {
    }

    public FareRule(VehicleCategory vehicleCategory, double baseFare, double perKilometreRate,
                    double minimumFare, double serviceCharge, double peakHourMultiplier, String currency) {
        this.vehicleCategory = vehicleCategory;
        this.baseFare = baseFare;
        this.perKilometreRate = perKilometreRate;
        this.minimumFare = minimumFare;
        this.serviceCharge = serviceCharge;
        this.peakHourMultiplier = peakHourMultiplier;
        this.currency = currency;
    }

    public VehicleCategory getVehicleCategory() {
        return vehicleCategory;
    }

    public void setVehicleCategory(VehicleCategory vehicleCategory) {
        this.vehicleCategory = vehicleCategory;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    public double getPerKilometreRate() {
        return perKilometreRate;
    }

    public void setPerKilometreRate(double perKilometreRate) {
        this.perKilometreRate = perKilometreRate;
    }

    public double getMinimumFare() {
        return minimumFare;
    }

    public void setMinimumFare(double minimumFare) {
        this.minimumFare = minimumFare;
    }

    public double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public double getPeakHourMultiplier() {
        return peakHourMultiplier;
    }

    public void setPeakHourMultiplier(double peakHourMultiplier) {
        this.peakHourMultiplier = peakHourMultiplier;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

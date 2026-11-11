package com.serendib.taxibooking.service;

import com.serendib.taxibooking.model.RouteEstimate;

public interface RoutingProvider {

    RouteEstimate route(double pickupLongitude, double pickupLatitude,
                        double destinationLongitude, double destinationLatitude);
}

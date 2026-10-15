package com.taxicab.integration.maps;

import com.taxicab.dto.route.RouteRequest;
import com.taxicab.model.Route;

public interface RoutingClient {
    Route route(RouteRequest routeRequest);
}

package com.serendib.taxibooking.service;

import com.serendib.taxibooking.config.FareConfig;
import com.serendib.taxibooking.dto.FareEstimateRequest;
import com.serendib.taxibooking.model.RouteEstimate;
import com.serendib.taxibooking.model.VehicleCategory;
import com.serendib.taxibooking.strategy.VehicleFareStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FareEstimateServiceTest {

    private final RoutingProvider routingProvider = mock(RoutingProvider.class);
    private final FareEstimateService service =
            new FareEstimateService(new FareConfig(), new VehicleFareStrategy(), routingProvider);

    @Test
    void estimatesFareFromRoadDistanceAndDuration() {
        when(routingProvider.route(0.0, 0.0, 1.0, 0.0))
                .thenReturn(new RouteEstimate(140_000, 4_800, List.of(
                        List.of(0.0, 0.0), List.of(0.5, 0.1), List.of(1.0, 0.0))));
        var estimate = service.estimate(new FareEstimateRequest(
                0.0, 0.0, 0.0, 1.0, VehicleCategory.CAR, false));

        assertThat(estimate.distanceKilometres()).isEqualByComparingTo("140.000");
        assertThat(estimate.durationMinutes()).isEqualTo(80);
        assertThat(estimate.estimatedFare()).isPositive();
        assertThat(estimate.currency()).isEqualTo("LKR");
        assertThat(estimate.distanceBasis()).contains("Road");
        assertThat(estimate.routeCoordinates()).hasSize(3);
    }

    @Test
    void failsIfRoadRouteIsUnavailableInsteadOfUsingStraightLineDistance() {
        when(routingProvider.route(79.8, 6.9, 79.9, 6.9))
                .thenThrow(new com.serendib.taxibooking.exception.ExternalProviderException(
                        "The road routing service is temporarily unavailable."));

        assertThatThrownBy(() -> service.estimate(new FareEstimateRequest(
                6.9, 79.8, 6.9, 79.9, VehicleCategory.CAR, false)))
                .isInstanceOf(com.serendib.taxibooking.exception.ExternalProviderException.class)
                .hasMessageContaining("routing service is temporarily unavailable");
    }

    @Test
    void rejectsOutOfRangeCoordinates() {
        assertThatThrownBy(() -> service.estimate(new FareEstimateRequest(
                91.0, 0.0, 0.0, 1.0, VehicleCategory.CAR, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");
    }
}

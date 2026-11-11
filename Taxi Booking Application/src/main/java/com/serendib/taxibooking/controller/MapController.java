package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.MapConfigurationResponse;
import com.serendib.taxibooking.dto.NearbyDriverResponse;
import com.serendib.taxibooking.dto.PlaceSuggestionResponse;
import com.serendib.taxibooking.service.GeocodingService;
import com.serendib.taxibooking.service.DriverLocationService;
import com.serendib.taxibooking.model.VehicleCategory;
import com.serendib.taxibooking.exception.ExternalProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maps")
public class MapController {

    private final GeocodingService geocodingService;
    private final DriverLocationService driverLocationService;
    private final RestClient restClient;
    private final String apiKey;
    private final String tileUrl;

    public MapController(GeocodingService geocodingService,
                         DriverLocationService driverLocationService,
                         RestClient.Builder restClientBuilder,
                         @Value("${app.maps.geoapify-api-key:}") String apiKey,
                         @Value("${app.maps.geoapify-tile-url:https://maps.geoapify.com/v1/tile/carto/{z}/{x}/{y}.png}") String tileUrl) {
        this.geocodingService = geocodingService;
        this.driverLocationService = driverLocationService;
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.tileUrl = tileUrl;
    }

    @GetMapping("/config")
    public MapConfigurationResponse mapConfiguration() {
        return new MapConfigurationResponse(!apiKey.isBlank(), "/api/maps/tiles/{z}/{x}/{y}.png");
    }

    @GetMapping("/search")
    public List<PlaceSuggestionResponse> searchPlaces(@RequestParam String q) {
        return geocodingService.search(q).stream()
                .map(place -> new PlaceSuggestionResponse(
                        place.address(), place.latitude(), place.longitude()))
                .toList();
    }

    @GetMapping("/drivers")
    public List<NearbyDriverResponse> nearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam VehicleCategory category) {
        return driverLocationService.findNearby(latitude, longitude, category);
    }

    @GetMapping(value = "/tiles/{z}/{x}/{y}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> tile(@org.springframework.web.bind.annotation.PathVariable int z,
                                       @org.springframework.web.bind.annotation.PathVariable int x,
                                       @org.springframework.web.bind.annotation.PathVariable int y) {
        if (apiKey.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        if (z < 0 || z > 20 || x < 0 || y < 0 || x >= (1 << z) || y >= (1 << z)) {
            return ResponseEntity.badRequest().build();
        }
        String tileUri = tileUrl
                .replace("{z}", Integer.toString(z))
                .replace("{x}", Integer.toString(x))
                .replace("{y}", Integer.toString(y));
        var uri = UriComponentsBuilder.fromUriString(tileUri)
                .queryParam("apiKey", apiKey)
                .build()
                .encode()
                .toUri();
        try {
            byte[] image = restClient.get().uri(uri).retrieve().body(byte[].class);
            if (image == null || image.length == 0) {
                throw new ExternalProviderException("The map tile service returned an empty image.");
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(12)).cachePublic())
                    .body(image);
        } catch (RestClientException exception) {
            throw new ExternalProviderException("The map tile service is temporarily unavailable.", exception);
        }
    }
}

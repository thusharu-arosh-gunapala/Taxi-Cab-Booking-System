const bookingForm = document.querySelector("#booking-form");

if (bookingForm) {
    const estimateButton = document.querySelector("#estimate-button");
    const estimateBox = document.querySelector("#fare-estimate");
    const mapContainer = document.querySelector("#booking-map");
    const mapHelp = document.querySelector("#map-help");
    const mapPlaceholder = document.querySelector("#map-placeholder");
    const coordinateDetails = document.querySelector(".coordinate-details");
    const endpointFields = {
        pickup: {
            address: document.querySelector("#pickupAddress"),
            latitude: document.querySelector("#pickupLatitude"),
            longitude: document.querySelector("#pickupLongitude"),
            search: document.querySelector("#pickupSearch"),
            suggestions: document.querySelector("#pickupSuggestions")
        },
        destination: {
            address: document.querySelector("#destinationAddress"),
            latitude: document.querySelector("#destinationLatitude"),
            longitude: document.querySelector("#destinationLongitude"),
            search: document.querySelector("#destinationSearch"),
            suggestions: document.querySelector("#destinationSuggestions")
        }
    };

    const refreshNearbyDrivers = async () => {
        if (!mapReady) {
            return;
        }
        const pickupLatitude = Number(endpointFields.pickup.latitude.value);
        const pickupLongitude = Number(endpointFields.pickup.longitude.value);
        const category = document.querySelector("#vehicleCategory").value;
        if (!Number.isFinite(pickupLatitude) || !Number.isFinite(pickupLongitude) || !category) {
            return;
        }
        if (nearbyRequest) {
            nearbyRequest.abort();
        }
        nearbyRequest = new AbortController();
        try {
            const query = new URLSearchParams({
                latitude: String(pickupLatitude),
                longitude: String(pickupLongitude),
                category
            });
            const response = await fetch(`/api/maps/drivers?${query}`, { signal: nearbyRequest.signal });
            const payload = await response.json();
            if (!response.ok) {
                throw new Error(payload.message || "Nearby drivers could not be loaded.");
            }
            nearbyMarkers.forEach((marker) => marker.remove());
            nearbyMarkers = payload.map((driver) =>
                new maplibregl.Marker({ color: "#16a34a", scale: 0.8 })
                    .setLngLat([driver.longitude, driver.latitude])
                    .setPopup(new maplibregl.Popup({ offset: 18 })
                        .setText(`${driver.vehicleCategory.replaceAll("_", " ")} · ${(driver.distanceMetres / 1000).toFixed(1)} km away`))
                    .addTo(map));
            mapHelp.textContent = payload.length
                ? `${payload.length} available ${category.replaceAll("_", " ").toLowerCase()} driver${payload.length === 1 ? "" : "s"} nearby.`
                : "No live nearby drivers reported yet.";
        } catch (error) {
            if (error.name !== "AbortError") {
                mapHelp.textContent = error.message || "Nearby drivers could not be loaded.";
            }
        }
    };

    function scheduleNearbyDrivers() {
        clearTimeout(nearbyRequestTimer);
        nearbyRequestTimer = setTimeout(refreshNearbyDrivers, 250);
    }

    let map;
    let mapReady = false;
    let activeEndpoint = "pickup";
    let routeSourceAvailable = false;
    let lastEstimateRequest = "";
    let markers = {};
    let nearbyMarkers = [];
    let nearbyRequest;
    let nearbyRequestTimer;

    const currentRequest = () => ({
        pickupLatitude: Number(endpointFields.pickup.latitude.value),
        pickupLongitude: Number(endpointFields.pickup.longitude.value),
        destinationLatitude: Number(endpointFields.destination.latitude.value),
        destinationLongitude: Number(endpointFields.destination.longitude.value),
        vehicleCategory: document.querySelector("#vehicleCategory").value,
        peakHour: document.querySelector("#peakHour").checked
    });

    const selectionIsValid = (request) =>
        bookingForm.checkValidity()
        && endpointFields.pickup.latitude.value.trim() !== ""
        && endpointFields.pickup.longitude.value.trim() !== ""
        && endpointFields.destination.latitude.value.trim() !== ""
        && endpointFields.destination.longitude.value.trim() !== ""
        && Number.isFinite(request.pickupLatitude)
        && Number.isFinite(request.pickupLongitude)
        && Number.isFinite(request.destinationLatitude)
        && Number.isFinite(request.destinationLongitude)
        && request.vehicleCategory;

    bookingForm.addEventListener("invalid", (event) => {
        if (coordinateDetails.contains(event.target)) {
            coordinateDetails.open = true;
        }
    }, true);

    const formatPlace = (place) => `${place.address} (${place.latitude.toFixed(5)}, ${place.longitude.toFixed(5)})`;

    const setEndpoint = (name, place, updateSearch = true) => {
        const fields = endpointFields[name];
        fields.address.value = place.address;
        fields.latitude.value = place.latitude;
        fields.longitude.value = place.longitude;
        if (updateSearch) {
            fields.search.value = place.address;
        }
        fields.suggestions.replaceChildren();
        placeMarker(name, [place.longitude, place.latitude]);
        if (name === "pickup") {
            scheduleNearbyDrivers();
        }
        updateEstimateState();
    };

    const placeMarker = (name, coordinates) => {
        if (!mapReady) {
            return;
        }
        if (markers[name]) {
            markers[name].setLngLat(coordinates);
            return;
        }
        const color = name === "pickup" ? "#2563eb" : "#06b6d4";
        const marker = new maplibregl.Marker({ color, draggable: true })
            .setLngLat(coordinates)
            .addTo(map);
        marker.on("dragend", () => {
            const point = marker.getLngLat();
            const fields = endpointFields[name];
            fields.latitude.value = point.lat.toFixed(6);
            fields.longitude.value = point.lng.toFixed(6);
            if (!fields.address.value.trim()) {
                fields.address.value = `${point.lat.toFixed(5)}, ${point.lng.toFixed(5)}`;
            }
            fields.search.value = fields.address.value;
            if (name === "pickup") {
                scheduleNearbyDrivers();
            }
            updateEstimateState();
        });
        markers[name] = marker;
    };

    const fitRoute = (coordinates) => {
        if (!mapReady || !coordinates || coordinates.length < 2) {
            return;
        }
        const source = map.getSource("booking-route");
        const geometry = { type: "LineString", coordinates };
        if (source) {
            source.setData({ type: "Feature", properties: {}, geometry });
        } else {
            map.addSource("booking-route", {
                type: "geojson",
                data: { type: "Feature", properties: {}, geometry }
            });
            map.addLayer({
                id: "booking-route-line",
                type: "line",
                source: "booking-route",
                layout: { "line-cap": "round", "line-join": "round" },
                paint: { "line-color": "#2563eb", "line-width": 5, "line-opacity": 0.9 }
            });
        }
        routeSourceAvailable = true;
        const bounds = coordinates.reduce(
            (result, coordinate) => result.extend(coordinate),
            new maplibregl.LngLatBounds(coordinates[0], coordinates[0])
        );
        map.fitBounds(bounds, { padding: 48, maxZoom: 15 });
    };

    const updateEstimateState = () => {
        const current = JSON.stringify(currentRequest());
        if (lastEstimateRequest && current !== lastEstimateRequest) {
            lastEstimateRequest = "";
            estimateBox.textContent = "Locations or ride options changed. Get an updated road route and fare.";
            if (routeSourceAvailable && map.getSource("booking-route")) {
                map.getSource("booking-route").setData({
                    type: "FeatureCollection",
                    features: []
                });
            }
        }
    };

    const fetchSuggestions = async (name, query) => {
        const fields = endpointFields[name];
        try {
            const response = await fetch(`/api/maps/search?q=${encodeURIComponent(query)}`);
            const payload = await response.json();
            if (!response.ok) {
                throw new Error(payload.message || "Address search is unavailable.");
            }
            fields.suggestions.replaceChildren();
            for (const place of payload) {
                const option = document.createElement("button");
                option.type = "button";
                option.className = "suggestion-option";
                option.setAttribute("role", "option");
                option.textContent = formatPlace(place);
                option.addEventListener("click", () => {
                    setEndpoint(name, place);
                    if (mapReady) {
                        map.flyTo({ center: [place.longitude, place.latitude], zoom: 14 });
                    }
                });
                fields.suggestions.append(option);
            }
            if (!payload.length) {
                const empty = document.createElement("p");
                empty.className = "suggestion-message";
                empty.textContent = "No matching places found.";
                fields.suggestions.replaceChildren(empty);
            }
        } catch (error) {
            fields.suggestions.replaceChildren();
            const message = document.createElement("p");
            message.className = "suggestion-message field-error";
            message.textContent = error.message || "Address search failed.";
            fields.suggestions.append(message);
        }
    };

    for (const [name, fields] of Object.entries(endpointFields)) {
        let searchTimer;
        fields.search.addEventListener("input", () => {
            clearTimeout(searchTimer);
            const query = fields.search.value.trim();
            if (query.length < 3) {
                fields.suggestions.replaceChildren();
                return;
            }
            searchTimer = setTimeout(() => fetchSuggestions(name, query), 300);
        });

        fields.latitude.addEventListener("input", updateEstimateState);
        fields.longitude.addEventListener("input", updateEstimateState);
        fields.address.addEventListener("input", updateEstimateState);
    }

    document.querySelector("#vehicleCategory").addEventListener("change", updateEstimateState);
    document.querySelector("#vehicleCategory").addEventListener("change", scheduleNearbyDrivers);
    document.querySelector("#peakHour").addEventListener("change", updateEstimateState);

    document.querySelector("#use-map-button").addEventListener("click", (event) => {
        activeEndpoint = activeEndpoint === "pickup" ? "destination" : "pickup";
        event.currentTarget.textContent = `Set ${activeEndpoint} on map`;
        mapHelp.textContent = `Click or drag a marker to select the ${activeEndpoint} location.`;
        if (mapReady) {
            map.getCanvas().style.cursor = "crosshair";
        }
    });

    const setupMap = async () => {
        try {
            const response = await fetch("/api/maps/config");
            if (!response.ok) {
                throw new Error("Map configuration could not be loaded.");
            }
            const configuration = await response.json();
            if (!configuration.enabled) {
                mapPlaceholder.textContent = "Set GEOAPIFY_API_KEY to enable the map, address search, and road routing.";
                return;
            }
            if (!window.maplibregl) {
                mapPlaceholder.textContent = "The map library could not be loaded. Check your internet connection and try again.";
                return;
            }

            const pickupLat = Number(endpointFields.pickup.latitude.value);
            const pickupLng = Number(endpointFields.pickup.longitude.value);
            const center = Number.isFinite(pickupLat) && Number.isFinite(pickupLng)
                ? [pickupLng, pickupLat]
                : [79.8612, 6.9271];
            map = new maplibregl.Map({
                container: "booking-map",
                style: {
                    version: 8,
                    sources: {
                        geoapify: {
                            type: "raster",
                            tiles: [configuration.tileUrl],
                            tileSize: 256,
                            attribution: "© Geoapify | © OpenStreetMap contributors"
                        }
                    },
                    layers: [{ id: "geoapify", type: "raster", source: "geoapify" }]
                },
                center,
                zoom: 12
            });
            map.addControl(new maplibregl.NavigationControl(), "top-right");
            map.on("load", () => {
                mapReady = true;
                mapContainer.classList.add("map-ready");
                mapPlaceholder.remove();
                for (const name of ["pickup", "destination"]) {
                    const fields = endpointFields[name];
                    const lat = Number(fields.latitude.value);
                    const lng = Number(fields.longitude.value);
                    if (Number.isFinite(lat) && Number.isFinite(lng)) {
                        placeMarker(name, [lng, lat]);
                    }
                }
                refreshNearbyDrivers();
                window.setInterval(refreshNearbyDrivers, 15000);
            });
            map.on("click", (event) => {
                if (!mapReady || map.getCanvas().style.cursor !== "crosshair") {
                    return;
                }
                const name = activeEndpoint;
                const fields = endpointFields[name];
                const lat = event.lngLat.lat;
                const lng = event.lngLat.lng;
                setEndpoint(name, {
                    address: `${lat.toFixed(5)}, ${lng.toFixed(5)}`,
                    latitude: lat,
                    longitude: lng
                }, true);
                map.getCanvas().style.cursor = "";
                mapHelp.textContent = `Selected ${name} location. Choose another marker or search for a place.`;
            });
        } catch (error) {
            mapPlaceholder.textContent = error.message || "The map could not be loaded.";
        }
    };

    estimateButton.addEventListener("click", async () => {
        const request = currentRequest();
        if (!selectionIsValid(request)) {
            coordinateDetails.open = true;
            bookingForm.reportValidity();
            return;
        }
        estimateBox.textContent = "Finding a road route and calculating the fare…";
        estimateButton.disabled = true;

        try {
            const response = await fetch("/api/fares/estimate", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(request)
            });
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.message || "A road route could not be calculated.");
            }
            estimateBox.textContent = `${result.distanceKilometres} km road route · ${result.durationMinutes} min · ${result.currency} ${Number(result.estimatedFare).toFixed(2)}`;
            lastEstimateRequest = JSON.stringify(request);
            fitRoute(result.routeCoordinates);
        } catch (error) {
            lastEstimateRequest = "";
            estimateBox.textContent = error.message || "A road route could not be calculated.";
        } finally {
            estimateButton.disabled = false;
        }
    });

    bookingForm.addEventListener("submit", (event) => {
        if (JSON.stringify(currentRequest()) !== lastEstimateRequest) {
            event.preventDefault();
            estimateBox.textContent = "Get a current road route and fare estimate before confirming this booking.";
            estimateButton.focus();
        }
    });

    setupMap();
}

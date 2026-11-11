# Project architecture

The application has one Maven root and one Spring Boot entry point at `com.serendib.taxibooking.TaxiBookingApplication`.

```text
Browser
  -> Thymeleaf pages / JavaScript
  -> Spring MVC controllers
  -> Services (booking rules, validation, fare estimates)
  -> Spring Data JPA repositories
  -> H2 for local development or configured MySQL
```

## Packages

- `config`: Spring Security and shared web configuration; migrated fare configuration.
- `controller`: home, authentication, passenger saved-place, booking, trip-history, fare, Geoapify map, driver application/portal, and authenticated driver integration endpoints.
- `dto`: validated passenger/driver application and password-change forms, booking, saved-place, fare, map, and driver-location request/response data.
- `entity`: JPA passenger, administrator, driver application/account, password-reset token, vehicle, favourite-location, booking, and trip records.
- `geo`, `model`, `strategy`: the migrated Haversine calculation and original fare-rule/category/strategy code.
- `repository`: Spring Data repositories; all implemented persistence goes through JPA.
- `service`: account registration, driver document storage and approval/email workflows, fare estimation, Geoapify geocoding/routing, short-lived nearby-driver locations, booking operations, saved locations, and trip operations.

## Current application behavior

- Public routes: `/`, `/login`, `/register`, and `POST /api/fares/estimate`.
- Authenticated routes: `/bookings`, `/bookings/new`, `/passenger/favourites`, and `/trips`.
- Registration and sign-in use Spring Security form login. Passwords are encoded with BCrypt; CSRF remains enabled for browser forms.
- Driver applications collect applicant and vehicle data plus private identity/vehicle images. Admins can inspect applications, approve or reject with a reason, and search/filter by status. Approval generates and BCrypt-hashes a temporary password, records email delivery status, and sends branded credentials after database commit; drivers must change the temporary password before any driver dashboard/availability access. Driver password reset uses one-time hashed tokens with a 30-minute expiry. The shared login uses `ROLE_DRIVER`; driver accounts are not a separate authentication system.
- Passenger registration retains the original full name, email, phone, and address fields. Saved places preserve the one-HOME/one-WORK rule.
- Booking creation calculates an estimated fare from supplied coordinates. No booking is reported as assigned: driver matching is not implemented.
- Booking cancellation is passenger-scoped. The trip service preserves acceptance/start/completion preconditions, but no driver-side controller can accept a booking yet.

## Integration boundaries and deferred work

The booking map uses MapLibre with Geoapify tiles, geocoding, and road routing. Geoapify credentials are configured server-side through `GEOAPIFY_API_KEY`, and map tiles are proxied by the application. Nearby-driver markers are based only on short-lived GPS updates received by the token-protected driver integration endpoint; an actual driver app/backend must send those updates. Coordinates are currently held in application memory and should move to shared expiring storage for multi-instance deployment. Driver ride dispatch/acceptance, driver-side trip controls, earnings, review/complaint administration, and the sixth member's assigned feature remain unimplemented; see the [migration report](migration-report.md).

Do not add another Spring Boot entry point or duplicate passenger/booking repositories. New team work belongs under this application root and must use the shared entities and service/repository boundaries.

package com.taxibooking.service; //UserService

import com.taxibooking.model.FavouriteLocation;
import com.taxibooking.model.Passenger;
import com.taxibooking.model.User;
import com.taxibooking.repository.FavouriteLocationRepository;
import com.taxibooking.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Passenger CRUD (register / view / update / deactivate / delete) and favourite locations. */
@Service
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{9,12}$");

    private final UserRepository userRepository;
    private final FavouriteLocationRepository favouriteRepository;
    private final AuthenticationService authService;

    public UserService(UserRepository userRepository,
                       FavouriteLocationRepository favouriteRepository,
                       AuthenticationService authService) {
        this.userRepository = userRepository;
        this.favouriteRepository = favouriteRepository;
        this.authService = authService;
    }

    // ---------------- CREATE ----------------
    public Passenger register(Map<String, String> p) {
        String name = val(p, "fullName");
        String email = val(p, "email").toLowerCase();
        String phone = val(p, "phone");
        String address = val(p, "address");
        String password = p.getOrDefault("password", "");
        String confirm = p.getOrDefault("confirmPassword", "");

        List<String> errors = new ArrayList<>();
        validateProfileFields(name, phone, address, errors);
        if (email.isEmpty()) errors.add("Email is required.");
        else if (!EMAIL.matcher(email).matches()) errors.add("Email format is invalid.");
        if (password.length() < AuthenticationService.MIN_PASSWORD_LENGTH)
            errors.add("Password must be at least " + AuthenticationService.MIN_PASSWORD_LENGTH + " characters.");
        if (!password.equals(confirm)) errors.add("Password and confirm password do not match.");
        if (errors.isEmpty() && userRepository.findByEmail(email).isPresent())
            errors.add("An account with this email already exists.");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        Passenger passenger = new Passenger();
        passenger.setUserId(userRepository.nextId("P"));
        passenger.setFullName(name);
        passenger.setEmail(email);
        passenger.setPhone(phone);
        passenger.setAddress(address);
        passenger.setPassword(authService.hash(password));
        passenger.setStatus(User.ACTIVE);
        userRepository.save(passenger);
        return passenger;
    }

    // ---------------- READ ----------------
    public Optional<Passenger> findPassenger(String id) {
        return userRepository.findById(id)
                .filter(u -> u instanceof Passenger)
                .map(u -> (Passenger) u);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // ---------------- UPDATE ----------------
    /** Email is the login ID so it is not editable here. */
    public Passenger updateProfile(String id, Map<String, String> p) {
        String name = val(p, "fullName");
        String phone = val(p, "phone");
        String address = val(p, "address");

        List<String> errors = new ArrayList<>();
        validateProfileFields(name, phone, address, errors);
        if (!errors.isEmpty()) throw new ValidationException(errors);

        Passenger passenger = findPassenger(id).orElseThrow(() -> new ValidationException("User not found."));
        passenger.setFullName(name);
        passenger.setPhone(phone);
        passenger.setAddress(address);
        userRepository.save(passenger);
        return passenger;
    }

    // ---------------- DELETE / DEACTIVATE ----------------
    /** Soft delete: the record stays in users.txt with status INACTIVE (keeps booking history valid). */
    public void deactivate(String id, String password) {
        Passenger passenger = findPassenger(id).orElseThrow(() -> new ValidationException("User not found."));
        if (!authService.matches(password, passenger.getPassword()))
            throw new ValidationException("Password is incorrect. Account was not deactivated.");
        passenger.setStatus(User.INACTIVE);
        userRepository.save(passenger);
    }

    /** Hard delete (e.g. for admin use). Also removes the passenger's favourite locations. */
    public void deletePermanently(String id) {
        for (FavouriteLocation f : favouriteRepository.findByPassengerId(id)) {
            favouriteRepository.deleteById(f.getLocationId());
        }
        userRepository.deleteById(id);
    }

    // ---------------- FAVOURITE LOCATIONS ----------------
    public List<FavouriteLocation> getFavourites(String passengerId) {
        return favouriteRepository.findByPassengerId(passengerId);
    }

    public Optional<FavouriteLocation> getFavourite(String passengerId, String locationId) {
        return favouriteRepository.findById(locationId)
                .filter(f -> f.getPassengerId().equals(passengerId));
    }

    public FavouriteLocation addFavourite(String passengerId, Map<String, String> p) {
        FavouriteLocation loc = new FavouriteLocation();
        loc.setPassengerId(passengerId);
        applyFavourite(loc, p);
        loc.setLocationId(favouriteRepository.nextId());
        return favouriteRepository.save(loc);
    }

    public FavouriteLocation updateFavourite(String passengerId, String locationId, Map<String, String> p) {
        FavouriteLocation loc = getFavourite(passengerId, locationId)
                .orElseThrow(() -> new ValidationException("Location not found."));
        applyFavourite(loc, p);
        return favouriteRepository.save(loc);
    }

    public void deleteFavourite(String passengerId, String locationId) {
        getFavourite(passengerId, locationId)
                .orElseThrow(() -> new ValidationException("Location not found."));
        favouriteRepository.deleteById(locationId);
    }

    // ---------------- helpers ----------------
    private void applyFavourite(FavouriteLocation loc, Map<String, String> p) {
        List<String> errors = new ArrayList<>();

        FavouriteLocation.Type type = null;
        try {
            type = FavouriteLocation.Type.valueOf(val(p, "type").toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add("Location type must be HOME, WORK or OTHER.");
        }

        String address = val(p, "address");
        if (address.isEmpty()) errors.add("Address is required.");

        double lat = 0, lng = 0;
        try {
            lat = Double.parseDouble(val(p, "latitude"));
            if (lat < -90 || lat > 90) errors.add("Latitude must be between -90 and 90.");
        } catch (NumberFormatException e) {
            errors.add("Latitude must be a number.");
        }
        try {
            lng = Double.parseDouble(val(p, "longitude"));
            if (lng < -180 || lng > 180) errors.add("Longitude must be between -180 and 180.");
        } catch (NumberFormatException e) {
            errors.add("Longitude must be a number.");
        }

        // only one HOME and one WORK per passenger
        if (type == FavouriteLocation.Type.HOME || type == FavouriteLocation.Type.WORK) {
            for (FavouriteLocation existing : favouriteRepository.findByPassengerId(loc.getPassengerId())) {
                boolean sameRecord = existing.getLocationId().equals(loc.getLocationId());
                if (!sameRecord && existing.getType() == type) {
                    errors.add("You already have a " + type + " location. Edit or delete it first.");
                    break;
                }
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);

        loc.setType(type);
        loc.setAddress(address);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
    }

    private void validateProfileFields(String name, String phone, String address, List<String> errors) {
        if (name.isEmpty()) errors.add("Full name is required.");
        if (phone.isEmpty()) errors.add("Phone number is required.");
        else if (!PHONE.matcher(phone).matches()) errors.add("Phone must be 9-12 digits (optional leading +).");
        if (address.isEmpty()) errors.add("Address is required.");
    }

    private String val(Map<String, String> p, String key) {
        String v = p.get(key);
        return v == null ? "" : v.trim();
    }
}


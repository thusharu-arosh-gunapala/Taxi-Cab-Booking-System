package com.taxibooking.service; //AuthenticationService

import com.taxibooking.model.User;
import com.taxibooking.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthenticationService {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return rawPassword != null && encoder.matches(rawPassword, hash);
    }

    /** Returns the user if credentials are valid and the account is ACTIVE. */
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email == null ? "" : email.trim()).orElse(null);
        if (user == null || !matches(password, user.getPassword())) {
            throw new ValidationException("Invalid email or password.");
        }
        if (!user.isActive()) {
            throw new ValidationException("This account has been deactivated.");
        }
        return user;
    }

    public void changePassword(String userId, String current, String newPw, String confirm) {
        List<String> errors = new ArrayList<>();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found."));

        if (!matches(current, user.getPassword())) errors.add("Current password is incorrect.");
        if (newPw == null || newPw.length() < MIN_PASSWORD_LENGTH)
            errors.add("New password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        else if (newPw.equals(current)) errors.add("New password must be different from the current password.");
        if (newPw == null || !newPw.equals(confirm)) errors.add("New password and confirmation do not match.");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        user.setPassword(hash(newPw));
        userRepository.save(user);
    }
}

package com.taxibooking.repository; //UserRepository

import com.taxibooking.model.Passenger;
import com.taxibooking.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** File-based storage for users.txt. All methods are synchronized so concurrent requests don't corrupt the file. */
@Repository
public class UserRepository {

    private final Path file;

    public UserRepository(@Value("${app.data-dir:data}") String dataDir) throws IOException {
        Path dir = Paths.get(dataDir);
        Files.createDirectories(dir);
        this.file = dir.resolve("users.txt");
        if (!Files.exists(file)) Files.createFile(file);
    }

    public synchronized List<User> findAll() {
        try {
            List<User> users = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) users.add(parse(line));
            }
            return users;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized Optional<User> findById(String id) {
        return findAll().stream().filter(u -> u.getUserId().equals(id)).findFirst();
    }

    public synchronized Optional<User> findByEmail(String email) {
        return findAll().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    /** Insert a new user, or replace the existing one with the same ID. */
    public synchronized User save(User user) {
        List<User> all = findAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getUserId().equals(user.getUserId())) {
                all.set(i, user);
                replaced = true;
                break;
            }
        }
        if (!replaced) all.add(user);
        writeAll(all);
        return user;
    }

    public synchronized void deleteById(String id) {
        List<User> all = findAll();
        all.removeIf(u -> u.getUserId().equals(id));
        writeAll(all);
    }

    /** Next ID for a prefix, e.g. "P" -> P001, P002 ... */
    public synchronized String nextId(String prefix) {
        int max = 0;
        for (User u : findAll()) {
            if (u.getUserId().startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(u.getUserId().substring(prefix.length())));
                } catch (NumberFormatException ignored) { }
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    private User parse(String line) {
        String[] p = line.split("\\|", -1);
        User u = Passenger.ROLE.equals(p[6]) ? new Passenger() : new User();
        u.setUserId(p[0]);
        u.setFullName(p[1]);
        u.setEmail(p[2]);
        u.setPhone(p[3]);
        u.setPassword(p[4]);
        u.setAddress(p[5]);
        u.setRole(p[6]);
        u.setStatus(p[7]);
        return u;
    }

    private void writeAll(List<User> users) {
        try {
            List<String> lines = new ArrayList<>();
            for (User u : users) lines.add(u.toFileLine());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

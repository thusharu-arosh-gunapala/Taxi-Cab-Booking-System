package com.taxibooking.repository; //FavouriteLocationRepository

import com.taxibooking.model.FavouriteLocation;
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

/** File-based storage for favourite_locations.txt */
@Repository
public class FavouriteLocationRepository {

    private final Path file;

    public FavouriteLocationRepository(@Value("${app.data-dir:data}") String dataDir) throws IOException {
        Path dir = Paths.get(dataDir);
        Files.createDirectories(dir);
        this.file = dir.resolve("favourite_locations.txt");
        if (!Files.exists(file)) Files.createFile(file);
    }

    public synchronized List<FavouriteLocation> findAll() {
        try {
            List<FavouriteLocation> list = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) list.add(parse(line));
            }
            return list;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized List<FavouriteLocation> findByPassengerId(String passengerId) {
        List<FavouriteLocation> result = new ArrayList<>();
        for (FavouriteLocation f : findAll()) {
            if (f.getPassengerId().equals(passengerId)) result.add(f);
        }
        return result;
    }

    public synchronized Optional<FavouriteLocation> findById(String id) {
        return findAll().stream().filter(f -> f.getLocationId().equals(id)).findFirst();
    }

    public synchronized FavouriteLocation save(FavouriteLocation loc) {
        List<FavouriteLocation> all = findAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getLocationId().equals(loc.getLocationId())) {
                all.set(i, loc);
                replaced = true;
                break;
            }
        }
        if (!replaced) all.add(loc);
        writeAll(all);
        return loc;
    }

    public synchronized void deleteById(String id) {
        List<FavouriteLocation> all = findAll();
        all.removeIf(f -> f.getLocationId().equals(id));
        writeAll(all);
    }

    public synchronized String nextId() {
        int max = 0;
        for (FavouriteLocation f : findAll()) {
            try {
                max = Math.max(max, Integer.parseInt(f.getLocationId().substring(1)));
            } catch (NumberFormatException ignored) { }
        }
        return String.format("F%03d", max + 1);
    }

    private FavouriteLocation parse(String line) {
        String[] p = line.split("\\|", -1);
        FavouriteLocation f = new FavouriteLocation();
        f.setLocationId(p[0]);
        f.setPassengerId(p[1]);
        f.setType(FavouriteLocation.Type.valueOf(p[2]));
        f.setAddress(p[3]);
        f.setLatitude(Double.parseDouble(p[4]));
        f.setLongitude(Double.parseDouble(p[5]));
        return f;
    }

    private void writeAll(List<FavouriteLocation> list) {
        try {
            List<String> lines = new ArrayList<>();
            for (FavouriteLocation f : list) lines.add(f.toFileLine());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

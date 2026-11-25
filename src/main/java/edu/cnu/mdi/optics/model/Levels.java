package edu.cnu.mdi.optics.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

/** Reads the levels shipped as {@code /levels/level-1.json}, {@code level-2.json} and so on. */
public final class Levels {

    private Levels() {
    }

    /** All levels, in play order. */
    public static List<Level> load() {
        List<Level> levels = new ArrayList<>();
        for (int number = 1; ; number++) {
            try (InputStream in = Levels.class.getResourceAsStream("/levels/level-" + number + ".json")) {
                if (in == null) {
                    break;
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    levels.add(new Gson().fromJson(reader, Level.class));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return List.copyOf(levels);
    }
}

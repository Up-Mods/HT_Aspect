package dev.upcraft.ht.aspect.util;

import com.google.common.flogger.LazyArgs;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class Env {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Map<String, Optional<String>> env = new ConcurrentHashMap<>();
    private static final List<Function<String, Optional<String>>> LOOKUPS = new ArrayList<>();

    static {
        LOGGER.atConfig().log("Loading environment variables...");
        LOOKUPS.add(key -> Optional.ofNullable(System.getenv(key)));
        lookupFromFile(Path.of(".env"));
        System.getenv().forEach((envKey, envValue) -> env.put(envKey, Optional.of(envValue)));

        // load files such as
        // production.env
        // development.env
        var envType = getOrDefault(EnvironmentType.ENV_KEY, () -> EnvironmentType.getDefault().name());
        lookupFromFile(Path.of("%s.env".formatted(envType)));
    }

    public static String getOrThrow(String key) {
        return lookup(key, Optional::empty).orElseThrow(() -> new IllegalStateException("Missing environment variable: '%s'".formatted(key)));
    }

    public static String getOrDefault(String key, Supplier<String> defaultValue) {
        return lookup(key, () -> {
            var value = Objects.requireNonNull(defaultValue.get(), () -> "Default value was null for environment variable: '%s'".formatted(key));
            return Optional.of(value);
        }).orElseThrow(() -> new IllegalStateException("Missing environment variable: '%s'".formatted(key)));
    }

    public static Optional<String> get(String key) {
        return lookup(key, Optional::empty);
    }

    private static Optional<String> lookup(String key, Supplier<Optional<String>> defaultValue) {
        return env.computeIfAbsent(key, s -> {
            for (Function<String, Optional<String>> lookup : LOOKUPS) {
                var opt = lookup.apply(s);
                if (opt.isPresent()) {
                    return opt;
                }
            }

            return Objects.requireNonNull(defaultValue.get());
        });
    }

    private static void lookupFromFile(Path envFile) {
        try (var reader = Files.newBufferedReader(envFile)) {
            LOGGER.atFine().log("Adding %s", LazyArgs.lazy(envFile::toAbsolutePath));
            var props = new Properties();
            props.load(reader);

            LOOKUPS.add(s -> Optional.ofNullable(props.getProperty(s)));
        } catch (IOException e) {
            if (!(e instanceof NoSuchFileException)) {
                LOGGER.atWarning().withCause(e).log("Unable to load additional environment from file %s", LazyArgs.lazy(envFile::toAbsolutePath));
            }
        }
    }
}

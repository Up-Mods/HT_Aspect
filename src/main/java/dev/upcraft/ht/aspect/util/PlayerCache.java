package dev.upcraft.ht.aspect.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.io.BlockingDiskFile;
import dev.upcraft.ht.aspect.api.auth.PlayerGameProfile;
import dev.upcraft.ht.aspect.api.types.Pair;
import dev.upcraft.ht.aspect.main.PluginMain;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

// FIXME make this async, not blocking
public class PlayerCache extends BlockingDiskFile {

    private static final int VERSION = 0;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClassFull();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final BiMap<UUID, String> cache = HashBiMap.create();

    public PlayerCache(JavaPlugin plugin) {
        super(plugin.getDataDirectory().resolve("playercache.json"));
    }
    public static PlayerCache get() {
        return PluginMain.getInstance().getPlayerCache();
    }

    public boolean updatePlayerProfile(UUID uuid, String username) {
        return this.modify(map -> {
            var prev = map.put(uuid, username);
            return !username.equals(prev);
        });
    }

    // TODO wrap in custom class to make this nicer
    private Optional<Pair<ProfileServiceClient, String>> getProfileLookup() {
        var authManager = ServerAuthManager.getInstance();
        if(!authManager.hasSessionToken()) {
            LOGGER.atWarning().atMostEvery(12, TimeUnit.HOURS).log("No session token available, unable to do profile lookup");
            return Optional.empty();
        }

        return Optional.of(new Pair<>(authManager.getProfileServiceClient(), authManager.getSessionToken()));
    }

    public CompletableFuture<PlayerGameProfile> getProfileForId(UUID id) {
        var cachedUsername = readUsernameForId(id);

        if(cachedUsername == null) {
            var lookup = getProfileLookup();
            if(lookup.isPresent()) {
                var profileService = lookup.get().first();
                var sessionToken = lookup.get().second();

                return profileService.getProfileByUuidAsync(id, sessionToken)
                        .thenApplyAsync(publicGameProfile -> {
                            if(publicGameProfile != null) {
                                updatePlayerProfile(publicGameProfile.getUuid(), publicGameProfile.getUsername());
                                return new PlayerGameProfile(publicGameProfile.getUuid(), publicGameProfile.getUsername());
                            }
                            return new PlayerGameProfile(id, null);
                        }, Universe.get().getDefaultWorld());
            }
        }

        return CompletableFuture.completedFuture(new PlayerGameProfile(id, cachedUsername));
    }

    public CompletableFuture<PlayerGameProfile> getProfileForUsername(String name) {
        var cachedId = readIdForUsername(name);

        if(cachedId == null) {
            var lookup = getProfileLookup();
            if(lookup.isPresent()) {
                var profileService = lookup.get().first();
                var sessionToken = lookup.get().second();

                return profileService.getProfileByUsernameAsync(name, sessionToken)
                        .thenApplyAsync(publicGameProfile -> {
                            if(publicGameProfile != null) {
                                updatePlayerProfile(publicGameProfile.getUuid(), publicGameProfile.getUsername());
                                return new PlayerGameProfile(publicGameProfile.getUuid(), publicGameProfile.getUsername());
                            }
                            return new PlayerGameProfile(null, name);
                        }, Universe.get().getDefaultWorld());
            }
        }

        return CompletableFuture.completedFuture(new PlayerGameProfile(cachedId, name));
    }

    @Deprecated(forRemoval = true)
    public CompletableFuture<@Nullable UUID> getIdForUsername(String name) {
        return getProfileForUsername(name).thenApply(profile -> profile.id().orElse(null));
    }

    private @Nullable String readUsernameForId(UUID id) {
        this.fileLock.readLock().lock();
        try {
            return this.cache.get(id);
        } finally {
            this.fileLock.readLock().unlock();
        }
    }

    private @Nullable UUID readIdForUsername(String name) {
        this.fileLock.readLock().lock();
        try {
            return this.cache.inverse().get(name);
        } finally {
            this.fileLock.readLock().unlock();
        }
    }

    private boolean modify(Predicate<BiMap<UUID, String>> function) {
        this.fileLock.writeLock().lock();

        boolean modified;
        try {
            modified = function.test(this.cache);
        } finally {
            this.fileLock.writeLock().unlock();
        }

        if (modified) {
            this.syncSave();
        }

        return modified;
    }

    @Override
    public void syncLoad() {
        if(!Options.getOptionSet().has(Options.BARE)) {
            try {
                Files.createDirectories(this.path.getParent());
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to create save directory!", e);
            }
        }
        super.syncLoad();
    }

    @Override
    protected void read(BufferedReader var1) throws IOException {
        this.cache.clear();

        var json = JsonParser.parseReader(var1).getAsJsonObject();
        var version = json.has("format_version") ? json.getAsJsonPrimitive("format_version").getAsInt() : 0;
        if(version > VERSION) {
            LOGGER.atSevere().log("Found unexpected player cache version %s, unable to parse. discarding data!!!", version);
            return;
        }

        try {
            json.getAsJsonArray("values").forEach(jsonElement -> {
                try {
                    var playerJson = jsonElement.getAsJsonObject();
                    var uuid = UUID.fromString(playerJson.getAsJsonPrimitive("uuid").getAsString());
                    var name = playerJson.getAsJsonPrimitive("name").getAsString();
                    this.cache.put(uuid, name);
                } catch (Exception e) {
                    LOGGER.atWarning().withCause(e).log("Serialization error: Unable to parse player cache entry!");
                }
            });
        } catch (Exception e) {
            throw new IOException("Unable to deserialize player cache!", e);
        }
    }

    @Override
    protected void write(BufferedWriter writer) throws IOException {
        var json = new JsonObject();
        json.addProperty("format_version", VERSION);
        var list = new JsonArray();
        this.cache.forEach((uuid, name) -> {
            var element = new JsonObject();
            element.addProperty("uuid", uuid.toString());
            element.addProperty("name", name);
            list.add(element);
        });
        json.add("values", list);
        GSON.toJson(json, writer);
    }

    @Override
    protected void create(BufferedWriter writer) throws IOException {
        var json = new JsonObject();
        json.addProperty("format_version", VERSION);
        json.add("values", new JsonArray());
        GSON.toJson(json, writer);
    }
}

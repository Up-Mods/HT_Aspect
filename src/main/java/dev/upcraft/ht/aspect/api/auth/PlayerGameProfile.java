package dev.upcraft.ht.aspect.api.auth;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerGameProfile {
    private final @Nullable UUID uuid;
    private final @Nullable String username;

    public PlayerGameProfile(@Nullable UUID uuid, @Nullable String username) {
        Preconditions.checkArgument(uuid != null || username != null);
        this.uuid = uuid;
        this.username = username;
    }

    public UUID unwrapId() {
        return Objects.requireNonNull(uuid, () -> "No UUID provided for %s".formatted(this));
    }

    public Optional<UUID> id() {
        return Optional.ofNullable(uuid);
    }

    public String unwrapUsername() {
        return Objects.requireNonNull(username, () -> "No username provided for %s".formatted(this));
    }

    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    public boolean isComplete() {
        return uuid != null && username != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlayerGameProfile) obj;
        return Objects.equals(this.uuid, that.uuid) &&
                Objects.equals(this.username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, username);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uuid", uuid)
                .add("username", username)
                .toString();
    }
}

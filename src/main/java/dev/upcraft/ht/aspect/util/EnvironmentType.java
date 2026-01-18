package dev.upcraft.ht.aspect.util;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class EnvironmentType {

    public static final EnvironmentType DEVELOPMENT = new EnvironmentType(InnerType.DEVELOPMENT, InnerType.DEVELOPMENT.getName());
    public static final EnvironmentType PRODUCTION = new EnvironmentType(InnerType.PRODUCTION, InnerType.PRODUCTION.getName());
    public static final String ENV_KEY = "HYTALE_ENVIRONMENT";
    private final InnerType innerType;
    private final String name;

    public EnvironmentType(InnerType innerType, String name) {
        this.innerType = innerType;
        this.name = name;
    }

    public static EnvironmentType fromString(String value) {
        if (PRODUCTION.name().equals(value)) {
            return PRODUCTION;
        } else if (DEVELOPMENT.name().equals(value)) {
            return DEVELOPMENT;
        }

        return new EnvironmentType(InnerType.UNKNOWN, value);
    }

    public static EnvironmentType getDefault() {
        return PRODUCTION;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (EnvironmentType) obj;
        return Objects.equals(this.innerType, that.innerType) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "EnvironmentType["
                + "name=" + name
                + ']';
    }

    public enum InnerType {
        DEVELOPMENT("development"),
        PRODUCTION("production"),
        UNKNOWN(null);

        private final String name;

        InnerType(@Nullable String name) {
            this.name = name;
        }

        private String getName() {
            return name;
        }
    }
}



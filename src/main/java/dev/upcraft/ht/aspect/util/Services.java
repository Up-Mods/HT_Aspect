package dev.upcraft.ht.aspect.util;

import com.hypixel.hytale.server.core.plugin.PluginManager;

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;

public class Services {

    public static <T> T load(Class<T> serviceClass) {
        return ServiceLoader.load(serviceClass, PluginManager.get().getBridgeClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find service %s".formatted(serviceClass.getName())));
    }

    public static <T> List<? extends T> loadAll(Class<T> serviceClass, Predicate<Class<? extends T>> filter) {
        return ServiceLoader.load(serviceClass, PluginManager.get().getBridgeClassLoader()).stream().filter(provider -> filter.test(provider.type())).map(ServiceLoader.Provider::get).toList();
    }
}

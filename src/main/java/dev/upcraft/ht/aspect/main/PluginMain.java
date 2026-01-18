package dev.upcraft.ht.aspect.main;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.upcraft.ht.aspect.util.PlayerCache;

public class PluginMain extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static PluginMain instance;
    private final PlayerCache playerCache = new PlayerCache(this);

    public PluginMain(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
    }

    @Override
    protected void start() {
        instance = this;
        playerCache.syncLoad();
    }

    @Override
    protected void shutdown() {
        playerCache.syncSave();
        instance = null;
    }

    public static PluginMain getInstance() {
        return instance;
    }

    public PlayerCache getPlayerCache() {
        return playerCache;
    }
}

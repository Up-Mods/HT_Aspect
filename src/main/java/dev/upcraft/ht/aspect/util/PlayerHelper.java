package dev.upcraft.ht.aspect.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.Universe;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerHelper {

    public static void broadcastMessageToAllPlayers(Message message) {
        broadcastMessageToAllPlayers(message, null);
    }

    public static void broadcastMessageToAllPlayers(Message message, @Nullable UUID senderId) {
        Universe.get().getWorlds().values().stream().flatMap(world -> world.getPlayerRefs().stream())
                .filter(playerRef -> senderId == null || !playerRef.getHiddenPlayersManager().isPlayerHidden(senderId))
                .forEach(playerRef -> playerRef.sendMessage(message));
        ConsoleSender.INSTANCE.sendMessage(message);
    }
}

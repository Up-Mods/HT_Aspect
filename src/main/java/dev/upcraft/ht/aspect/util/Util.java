package dev.upcraft.ht.aspect.util;

import java.util.UUID;

public class Util {

    public static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static <T> T TODO() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}

package fr.gouv.stopc.robertserver.common;

import java.util.HashMap;
import java.util.Map;

public enum RobertRequestType {

    HELLO((byte) 0x01),
    STATUS((byte) 0x02),
    UNREGISTER((byte) 0x03),
    DELETE_HISTORY((byte) 0x04);

    private final byte salt;

    private static Map map = new HashMap<>();

    RobertRequestType(byte salt) {
        this.salt = salt;
    }

    public byte getValue() {
        return salt;
    }

    static {
        for (RobertRequestType digestSalt : RobertRequestType.values()) {
            map.put(digestSalt.salt, digestSalt);
        }
    }

    public static RobertRequestType valueOf(byte val) {
        return (RobertRequestType) map.get(val);
    }
}

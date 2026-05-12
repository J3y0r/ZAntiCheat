package cn.jeyor1337.zanticheat.event.packetrecive.packettype;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PacketTypeRecognizer {

    public static PacketType getPacketType(Object nmsPacket) {
        String className = getPacketClassName(nmsPacket);
        switch (className) {
            case "PacketPlayInFlying":
            case "PacketPlayInPosition":
            case "PacketPlayInLook":
            case "PacketPlayInPositionLook":
            case "ServerboundMovePlayerPacket":
            case "ServerboundMovePlayerPosPacket":
            case "ServerboundMovePlayerPosRotPacket":
            case "ServerboundMovePlayerRotPacket":
            case "ServerboundMovePlayerStatusOnlyPacket":
                return PacketType.FLYING;
            case "PacketPlayInArmAnimation":
            case "ServerboundSwingPacket":
                return PacketType.ARM_ANIMATION;
            case "PacketPlayInBlockDig":
            case "ServerboundPlayerActionPacket":
                return PacketType.BLOCK_DIG;
            case "PacketPlayInSteerVehicle":
            case "ServerboundPlayerInputPacket":
                return PacketType.STEER_VEHICLE;
            case "PacketPlayInSetCreativeSlot":
            case "ServerboundSetCreativeModeSlotPacket":
                return PacketType.SET_CREATIVE_SLOT;
            case "ServerboundClientInformationPacket":
            case "PacketPlayInSettings":
                return PacketType.CLIENT_INFORMATION;
            case "PacketPlayInHeldItemSlot":
            case "ServerboundSetCarriedItemPacket":
                return PacketType.HELD_ITEM_SLOT;
            case "ServerboundKeepAlivePacket":
            case "PacketPlayInKeepAlive":
                return PacketType.ALIVE;
            case "PacketPlayInUseEntity":
            case "ServerboundInteractPacket":
                return PacketType.USE_ENTITY;
            default:
                return PacketType.OTHER;
        }
    }

    public static int getEntityId(Object nmsPacket) {
        if (getPacketType(nmsPacket) != PacketType.USE_ENTITY)
            return 0;
        Field[] fields = nmsPacket.getClass().getDeclaredFields();
        if (fields.length > 4 + 1) {
            return 0;
        }
        for (Field field : fields) {
            boolean accessible = field.isAccessible();
            if (!accessible) {
                field.setAccessible(true);
            }
            try {
                Object object = field.get(nmsPacket);
                if (object instanceof Integer) {
                    int value = (int) object;
                    if (value != 0) {
                        if (!accessible) {
                            field.setAccessible(false);
                        }
                        return value;
                    }
                }
            } catch (IllegalAccessException ignored) {
            }
            if (!accessible) {
                field.setAccessible(false);
            }
        }
        return 0;
    }

    public static boolean isAttack(Object nmsPacket) {
        if (getPacketType(nmsPacket) != PacketType.USE_ENTITY)
            return false;
        Object action = getUseEntityAction(nmsPacket);
        if (action == null)
            return false;
        return "ATTACK".equals(String.valueOf(action));
    }

    private static Object getUseEntityAction(Object nmsPacket) {
        for (Method method : nmsPacket.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() != 0)
                continue;
            Class<?> returnType = method.getReturnType();
            if (!returnType.isEnum())
                continue;
            boolean accessible = method.isAccessible();
            if (!accessible) {
                method.setAccessible(true);
            }
            try {
                Object action = method.invoke(nmsPacket);
                if (isUseEntityAction(action)) {
                    if (!accessible) {
                        method.setAccessible(false);
                    }
                    return action;
                }
            } catch (ReflectiveOperationException ignored) {
            }
            if (!accessible) {
                method.setAccessible(false);
            }
        }

        for (Field field : nmsPacket.getClass().getDeclaredFields()) {
            boolean accessible = field.isAccessible();
            if (!accessible) {
                field.setAccessible(true);
            }
            try {
                Object action = field.get(nmsPacket);
                if (isUseEntityAction(action)) {
                    if (!accessible) {
                        field.setAccessible(false);
                    }
                    return action;
                }
            } catch (IllegalAccessException ignored) {
            }
            if (!accessible) {
                field.setAccessible(false);
            }
        }
        return null;
    }

    private static boolean isUseEntityAction(Object action) {
        if (!(action instanceof Enum<?>))
            return false;
        String name = ((Enum<?>) action).name();
        return "ATTACK".equals(name) || "INTERACT".equals(name) || "INTERACT_AT".equals(name);
    }

    private static String getPacketClassName(Object nmsPacket) {
        String className = nmsPacket.getClass().getName();
        return className.split("\\.")[className.split("\\.").length - 1].split("\\$")[0];
    }

}

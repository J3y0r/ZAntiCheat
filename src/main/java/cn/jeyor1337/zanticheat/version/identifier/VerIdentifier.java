package cn.jeyor1337.zanticheat.version.identifier;

import org.bukkit.Bukkit;

public class VerIdentifier {

    private static ZACVersion serverVersion = null;

    public static ZACVersion getVersion() {
        if (serverVersion != null)
            return serverVersion;

        String name = Bukkit.getServer().getClass().getPackage().getName();
        String version = name.substring(name.lastIndexOf('.') + 1);
        if (version.startsWith("v1_8"))
            serverVersion = ZACVersion.V1_8;
        else if (version.startsWith("v1_9"))
            serverVersion = ZACVersion.V1_9;
        else if (version.startsWith("v1_10"))
            serverVersion = ZACVersion.V1_10;
        else if (version.startsWith("v1_11"))
            serverVersion = ZACVersion.V1_11;
        else if (version.startsWith("v1_12"))
            serverVersion = ZACVersion.V1_12;
        else if (version.startsWith("v1_13"))
            serverVersion = ZACVersion.V1_13;
        else if (version.startsWith("v1_14"))
            serverVersion = ZACVersion.V1_14;
        else if (version.startsWith("v1_15"))
            serverVersion = ZACVersion.V1_15;
        else if (version.startsWith("v1_16"))
            serverVersion = ZACVersion.V1_16;
        else if (version.startsWith("v1_17"))
            serverVersion = ZACVersion.V1_17;
        else if (version.startsWith("v1_18"))
            serverVersion = ZACVersion.V1_18;
        else if (version.startsWith("v1_19"))
            serverVersion = ZACVersion.V1_19;
        else if (version.startsWith("v1_20"))
            serverVersion = ZACVersion.V1_20;
        else if (version.startsWith("v1_21"))
            serverVersion = ZACVersion.V1_21;
        else serverVersion = ZACVersion.V1_21;
        return serverVersion;
    }

}

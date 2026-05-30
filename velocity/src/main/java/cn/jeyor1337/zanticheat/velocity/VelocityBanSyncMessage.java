package cn.jeyor1337.zanticheat.velocity;

public final class VelocityBanSyncMessage {

    public final String serverId;
    public final String playerName;
    public final String uuid;
    public final String checkName;
    public final String command;
    public final long timestamp;

    public VelocityBanSyncMessage(String serverId, String playerName, String uuid, String checkName, long timestamp) {
        this(serverId, playerName, uuid, checkName, "", timestamp);
    }

    public VelocityBanSyncMessage(String serverId, String playerName, String uuid, String checkName, String command, long timestamp) {
        this.serverId = serverId;
        this.playerName = playerName;
        this.uuid = uuid;
        this.checkName = checkName;
        this.command = command;
        this.timestamp = timestamp;
    }

    public static VelocityBanSyncMessage deserialize(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5 && parts.length != 6) {
            return null;
        }
        try {
            if (parts.length == 5) {
                return new VelocityBanSyncMessage(parts[0], parts[1], parts[2], parts[3], Long.parseLong(parts[4]));
            }
            return new VelocityBanSyncMessage(parts[0], parts[1], parts[2], parts[3], parts[4], Long.parseLong(parts[5]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

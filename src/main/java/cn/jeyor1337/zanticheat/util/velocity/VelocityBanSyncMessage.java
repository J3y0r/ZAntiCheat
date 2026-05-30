package cn.jeyor1337.zanticheat.util.velocity;

public class VelocityBanSyncMessage {

    public final String serverId;
    public final String playerName;
    public final String uuid;
    public final String checkName;
    public final long timestamp;

    public VelocityBanSyncMessage(String serverId, String playerName, String uuid, String checkName, long timestamp) {
        this.serverId = serverId;
        this.playerName = playerName;
        this.uuid = uuid;
        this.checkName = checkName;
        this.timestamp = timestamp;
    }

}

package cn.jeyor1337.zanticheat.util.velocity;

public interface VelocityMessageBridge {

    boolean initialize();

    boolean publish(VelocityBanSyncMessage message);

    void subscribe(VelocityMessageConsumer consumer);

    void close();

}

package cn.jeyor1337.zanticheat.version.identifier;

public enum ZACVersion {

    V1_8(0),
    V1_9(1),
    V1_10(3),
    V1_11(4),
    V1_12(5),
    V1_13(6),
    V1_14(7),
    V1_15(8),
    V1_16(9),
    V1_17(10),
    V1_18(11),
    V1_19(12),
    V1_20(13),
    V1_21(14);

    public final int number;

    ZACVersion(int number) {
        this.number = number;
    }

    public boolean isOlderThan(ZACVersion lacVersion) {
        return this.number < lacVersion.number;
    }

    public boolean isNewerThan(ZACVersion lacVersion) {
        return this.number > lacVersion.number;
    }

    public boolean isOlderOrEqualsTo(ZACVersion lacVersion) {
        return this.number <= lacVersion.number;
    }

    public boolean isNewerOrEqualsTo(ZACVersion lacVersion) {
        return this.number >= lacVersion.number;
    }

}

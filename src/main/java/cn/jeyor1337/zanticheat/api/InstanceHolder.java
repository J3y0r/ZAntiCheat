package cn.jeyor1337.zanticheat.api;

public class InstanceHolder {

    private static ZACApi api;

    public static void setApi(ZACApi api) {
        InstanceHolder.api = api;
    }

    public static ZACApi getApi() {
        return api;
    }

}

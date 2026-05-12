package cn.jeyor1337.zanticheat.api;

public class InstanceHolder {

    private static LACApi api;

    public static void setApi(LACApi api) {
        InstanceHolder.api = api;
    }

    public static LACApi getApi() {
        return api;
    }

}

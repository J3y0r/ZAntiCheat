package cn.jeyor1337.zanticheat.util.precise;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.version.identifier.ZACVersion;
import cn.jeyor1337.zanticheat.version.identifier.VerIdentifier;

public class AccuracyUtil {

    public static boolean isViolationCancel(CheckSetting checkSetting, Buffer buffer) {
        CheckName checkName = checkSetting.name;
        if (VerIdentifier.getVersion().isNewerThan(ZACVersion.V1_8)) {
            if (checkName == CheckName.FLIGHT_A && getRecentViolations(buffer) <= 5)
                return true;
            if (checkName == CheckName.FLIGHT_C && getRecentViolations(buffer) <= 3)
                return true;
            if (checkName == CheckName.SPEED_B && getRecentViolations(buffer) <= 3)
                return true;
            if (checkName == CheckName.SPEED_C && getRecentViolations(buffer) <= 2)
                return true;
        } else {
            if (checkName == CheckName.FLIGHT_A && getRecentViolations(buffer) <= 7)
                return true;
            if (checkName == CheckName.FLIGHT_C && getRecentViolations(buffer) <= 4)
                return true;
            if (checkName == CheckName.SPEED_B && getRecentViolations(buffer) <= 4)
                return true;
            if (checkName == CheckName.SPEED_C && getRecentViolations(buffer) <= 3)
                return true;
            if (checkName == CheckName.SPEED_A && getRecentViolations(buffer) <= 2)
                return true;
            if (checkName == CheckName.KILLAURA_B && getRecentViolations(buffer) <= 1)
                return true;
        }
        return false;
    }

    private static int getRecentViolations(Buffer buffer) {
        buffer.put("accuracyUtilViolations", buffer.getInt("accuracyUtilViolations") + 1);
        if (System.currentTimeMillis() - buffer.getLong("accuracyUtilTime") > 5000) {
            buffer.put("accuracyUtilTime", System.currentTimeMillis());
            buffer.put("accuracyUtilViolations", 1);
        }
        return buffer.getInt("accuracyUtilViolations");
    }

}

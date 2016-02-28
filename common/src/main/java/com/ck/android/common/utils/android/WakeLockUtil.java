package com.ck.android.common.utils.android;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

/**
 * <!-- 亮屏 -->
 * require <uses-permission android:name="android.permission.WAKE_LOCK"/>
 *
 * @author MaTianyu
 * @date 2014-11-04
 */
public class WakeLockUtil {
    PowerManager          powerManager;
    PowerManager.WakeLock wakeLock;

    public WakeLockUtil(Context context, String tag) {
        ////获取电源的服务 声明电源管理器
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, tag);
    }

    /**
     * Call requires API level 7
     */
    public boolean isScreenOn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR_MR1) {
            LogUtil.getInstance().e("can not call isScreenOn if SDK_INT < 7 ");
            return false;
        } else {
            return powerManager.isScreenOn();
        }
    }

    public void turnScreenOn() {
        //点亮亮屏
        LogUtil.getInstance().i("PowerManager.WakeLockUtil : wakeLock.isHeld: " + wakeLock.isHeld());
        if (!wakeLock.isHeld()) {
            LogUtil.getInstance().i("PowerManager.WakeLockUtil : 点亮屏幕");
            wakeLock.acquire();
        }
    }

    public void turnScreenOff() {
        //释放亮屏
        LogUtil.getInstance().i("PowerManager.WakeLockUtil : wakeLock.isHeld: " + wakeLock.isHeld());
        if (wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void release() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PowerManager.WakeLock getWakeLock() {
        return wakeLock;
    }

    public void setWakeLock(PowerManager.WakeLock wakeLock) {
        this.wakeLock = wakeLock;
    }

    public PowerManager getPowerManager() {
        return powerManager;
    }

    public void setPowerManager(PowerManager powerManager) {
        this.powerManager = powerManager;
    }
}
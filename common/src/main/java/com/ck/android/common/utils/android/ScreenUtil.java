package com.ck.android.common.utils.android;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * ScreenUtil
 * <ul>
 * <strong>Convert between dp and sp</strong>
 * <li>{@link ScreenUtil#dpToPx(Context, float)}</li>
 * <li>{@link ScreenUtil#pxToDp(Context, float)}</li>
 * </ul>
 *
 * @author <a href="http://www.trinea.cn" target="_blank">Trinea</a> 2014-2-14
 */
public class ScreenUtil {

    private ScreenUtil() {
        throw new AssertionError();
    }

    public static float dpToPx(Context context, float dp) {
        if (context == null) {
            return -1;
        }
        return dp * getDensity(context);
    }

    public static float pxToDp(Context context, float px) {
        if (context == null) {
            return -1;
        }
        return px /getDensity(context);
    }

    public static int dpToPxInt(Context context, float dp) {
        return (int) (dpToPx(context, dp) + 0.5f);
    }

    public static int pxToDpCeilInt(Context context, float px) {
        return (int) (pxToDp(context, px) + 0.5f);
    }


    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }


    /**
     * 获取 显示信息
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm;
    }


    /**
     * 打印 显示信息
     */
    public static DisplayMetrics printDisplayInfo(Context context) {
        DisplayMetrics dm = getDisplayMetrics(context);

        StringBuilder sb = new StringBuilder();
        sb.append("_______  显示信息:  ");
        sb.append("\ndensity         :").append(dm.density);
        sb.append("\ndensityDpi      :").append(dm.densityDpi);
        sb.append("\nheightPixels    :").append(dm.heightPixels);
        sb.append("\nwidthPixels     :").append(dm.widthPixels);
        sb.append("\nscaledDensity   :").append(dm.scaledDensity);
        sb.append("\nxdpi            :").append(dm.xdpi);
        sb.append("\nydpi            :").append(dm.ydpi);
        LogUtil.getInstance().i(sb.toString());

        return dm;
    }

}

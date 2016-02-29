package com.ck.android.common.utils.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

/**
 * @author MaTianyu
 * @date 2014-07-31
 */
public class ToastUtil {


    private static InternalHandler sHandler;


    private static class InternalHandler extends Handler {
        public InternalHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {


        }
    }


    private static Handler getHandler() {
        synchronized (ToastUtil.class) {
            if (sHandler == null) {
                sHandler = new InternalHandler();
            }
            return sHandler;
        }
    }


    public static void showShortToast(final Context context, final String text) {

        getHandler().post(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

            }
        });

    }

    public static void showShortToast(final Context context, final int resourceId) {

        getHandler().post(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(context, resourceId, Toast.LENGTH_SHORT).show();

            }
        });

    }

    public static void showLongToast(final Context context, final String text) {

        getHandler().post(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(context, text, Toast.LENGTH_LONG).show();

            }
        });

    }

    public static void showLongToast(final Context context, final int resourceId) {

        getHandler().post(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(context, resourceId, Toast.LENGTH_LONG).show();

            }
        });

    }


}

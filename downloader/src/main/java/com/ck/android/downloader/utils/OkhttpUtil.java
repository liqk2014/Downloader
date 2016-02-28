package com.ck.android.downloader.utils;

import com.ck.android.downloader.Constants;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by liqk on 16/2/28.
 */
public class OkhttpUtil {


    private static  OkHttpClient okHttpClient;




   public static OkHttpClient getOkHttpClient() {

       if (okHttpClient==null) {

           okHttpClient = new OkHttpClient.Builder().connectTimeout(Constants.HTTP.CONNECT_TIME_OUT, TimeUnit.SECONDS).addInterceptor(new StethoInterceptor()).build();
       }

       return okHttpClient;

    }



}

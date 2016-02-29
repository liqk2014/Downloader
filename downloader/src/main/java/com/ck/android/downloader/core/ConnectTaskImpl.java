package com.ck.android.downloader.core;

import android.os.Process;
import android.text.TextUtils;

import com.ck.android.downloader.Constants;
import com.ck.android.downloader.DownloadException;
import com.ck.android.downloader.architecture.ConnectTask;
import com.ck.android.downloader.architecture.DownloadStatus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by Aspsine on 2015/7/20.
 */
public class ConnectTaskImpl implements ConnectTask {
    private final String mUri;
    private final OnConnectListener mOnConnectListener;

    private volatile int mStatus;

    private volatile long mStartTime;

    public ConnectTaskImpl(String uri, OnConnectListener listener) {
        this.mUri = uri;
        this.mOnConnectListener = listener;
    }

    public void cancel() {
        mStatus = DownloadStatus.STATUS_CANCELED;
    }

    @Override
    public boolean isConnecting() {
        return mStatus == DownloadStatus.STATUS_CONNECTING;
    }

    @Override
    public boolean isConnected() {
        return mStatus == DownloadStatus.STATUS_CONNECTED;
    }

    @Override
    public boolean isCanceled() {
        return mStatus == DownloadStatus.STATUS_CANCELED;
    }

    @Override
    public boolean isFailed() {
        return mStatus == DownloadStatus.STATUS_FAILED;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mStatus = DownloadStatus.STATUS_CONNECTING;
        mOnConnectListener.onConnecting();
        try {
            executeConnection();
        } catch (DownloadException e) {
            handleDownloadException(e);
        }
    }

    private void executeConnection() throws DownloadException {
        mStartTime = System.currentTimeMillis();
        HttpURLConnection httpConnection = null;
        final URL url;
        try {
            url = new URL(mUri);
        } catch (MalformedURLException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.", e);
        }
        try {
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(Constants.HTTP.CONNECT_TIME_OUT);
            httpConnection.setReadTimeout(Constants.HTTP.READ_TIME_OUT);
            httpConnection.setRequestMethod(Constants.HTTP.GET);
            httpConnection.setRequestProperty("Range", "bytes=" + 0 + "-");
            final int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                parseResponse(httpConnection, false);
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                parseResponse(httpConnection, true);
            } else {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "UnSupported response code:" + responseCode);
            }
        } catch (ProtocolException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Protocol error", e);
        } catch (IOException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "IO error", e);
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

//    private void executeConnection() throws DownloadException {
//        mStartTime = System.currentTimeMillis();
//
//
//        OkHttpClient okHttpClient = OkhttpUtil.getOkHttpClient();
//
//
//        Request request = new Request.Builder().url(mUri).addHeader("Range", "bytes=" + 0 + "-").build();
//        Response response = null;
//
//        try {
//            response = okHttpClient.newCall(request).execute();
//            if (response == null) {
//                throw new DownloadException(DownloadStatus.STATUS_FAILED, "response==null");
//
//            }
//
//            final int responseCode = response.code();
//
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                parseResponse(response, false);
//            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
//                parseResponse(response, true);
//            } else {
//                throw new DownloadException(DownloadStatus.STATUS_FAILED, "UnSupported response code:" + responseCode);
//            }
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.", e);
//        }


//    }


    private void executeOkhttpConnection() throws DownloadException {


    }

    private void parseResponse(HttpURLConnection httpConnection, boolean isAcceptRanges) throws DownloadException {

        final long length;
        String contentLength = httpConnection.getHeaderField("Content-Length");
        if (TextUtils.isEmpty(contentLength) || contentLength.equals("0") || contentLength.equals("-1")) {
            length = httpConnection.getContentLength();
        } else {
            length = Long.parseLong(contentLength);
        }

        if (length <= 0) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "length <= 0");
        }

        if(checkCanceled())
            return;


        //Successful
        mStatus = DownloadStatus.STATUS_CONNECTED;
        final long timeDelta = System.currentTimeMillis() - mStartTime;
        mOnConnectListener.onConnected(timeDelta, length, isAcceptRanges);
    }


//    private void parseResponse(Response response, boolean isAcceptRanges) throws DownloadException {
//
//
//
//        long length = Long.parseLong(response.header("Content-Length", "-1"));
//
//
//        if (length <= 0) {
//            throw new DownloadException(DownloadStatus.STATUS_FAILED, "length <= 0");
//        }
//
//        checkCanceled();
//
//        //Successful
//        mStatus = DownloadStatus.STATUS_CONNECTED;
//        final long timeDelta = System.currentTimeMillis() - mStartTime;
//        mOnConnectListener.onConnected(timeDelta, length, isAcceptRanges);
//    }

//    private void checkCanceled() throws DownloadException {
//        if (isCanceled()) {
//            // cancel
//            throw new DownloadException(DownloadStatus.STATUS_CANCELED, "Download paused!");
//        }
//    }

    private boolean checkCanceled() throws DownloadException {
        if (isCanceled()) {
            // cancel
//            throw new DownloadException(DownloadStatus.STATUS_CANCELED, "Download paused!");

            handleDownloadException(new DownloadException(DownloadStatus.STATUS_CANCELED, "Download paused!"));
         return true;
        }
        return false;
    }

    private void handleDownloadException(DownloadException e) {
        switch (e.getErrorCode()) {
            case DownloadStatus.STATUS_FAILED:
                synchronized (mOnConnectListener) {
                    mStatus = DownloadStatus.STATUS_FAILED;
                    mOnConnectListener.onConnectFailed(e);
                }
                break;
            case DownloadStatus.STATUS_CANCELED:
                synchronized (mOnConnectListener) {
                    mStatus = DownloadStatus.STATUS_CANCELED;
                    mOnConnectListener.onConnectCanceled();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown state");
        }
    }
}

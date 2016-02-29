package com.ck.android.downloader.core;


import android.os.Process;
import android.text.TextUtils;

import com.ck.android.common.utils.android.LogUtil;
import com.ck.android.common.utils.java.IOUtil;
import com.ck.android.downloader.Constants;
import com.ck.android.downloader.DownloadException;
import com.ck.android.downloader.DownloadInfo;
import com.ck.android.downloader.architecture.DownloadStatus;
import com.ck.android.downloader.architecture.DownloadTask;
import com.ck.android.downloader.db.ThreadInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Created by Aspsine on 2015/7/27.
 */
public abstract class DownloadTaskImpl implements DownloadTask {

    private String mTag;

    private final DownloadInfo mDownloadInfo;
    private final ThreadInfo mThreadInfo;
    private final OnDownloadListener mOnDownloadListener;

    private volatile int mStatus;

    private volatile int mCommend = 0;

    public DownloadTaskImpl(DownloadInfo downloadInfo, ThreadInfo threadInfo, OnDownloadListener listener) {
        this.mDownloadInfo = downloadInfo;
        this.mThreadInfo = threadInfo;
        this.mOnDownloadListener = listener;

        this.mTag = getTag();
        if (TextUtils.isEmpty(mTag)) {
            mTag = this.getClass().getSimpleName();
        }
    }

    @Override
    public void cancel() {
        mCommend = DownloadStatus.STATUS_CANCELED;
    }

    @Override
    public void pause() {
        mCommend = DownloadStatus.STATUS_PAUSED;
    }

    @Override
    public boolean isDownloading() {
        return mStatus == DownloadStatus.STATUS_PROGRESS;
    }

    @Override
    public boolean isComplete() {
        return mStatus == DownloadStatus.STATUS_COMPLETED;
    }

    @Override
    public boolean isPaused() {
        return mStatus == DownloadStatus.STATUS_PAUSED;
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
        insertIntoDB(mThreadInfo);
        try {
            mStatus = DownloadStatus.STATUS_PROGRESS;
            executeDownload();

            if (checkPausedOrCanceled())
                return;

            synchronized (mOnDownloadListener) {
                mStatus = DownloadStatus.STATUS_COMPLETED;
                mOnDownloadListener.onDownloadCompleted();
            }
        } catch (DownloadException e) {
            LogUtil.getInstance().d("catch DownloadException:DownloadStatus:" + e.getErrorMessage());

            handleDownloadException(e);
        }
    }

    private void handleDownloadException(DownloadException e) {
        switch (e.getErrorCode()) {
            case DownloadStatus.STATUS_FAILED:
                synchronized (mOnDownloadListener) {
                    mStatus = DownloadStatus.STATUS_FAILED;
                    mOnDownloadListener.onDownloadFailed(e);
                }
                break;
            case DownloadStatus.STATUS_PAUSED:
                synchronized (mOnDownloadListener) {

                    LogUtil.getInstance().d("handleDownloadException:DownloadStatus.STATUS_PAUSED");
                    mStatus = DownloadStatus.STATUS_PAUSED;
                    mOnDownloadListener.onDownloadPaused();
                }
                break;
            case DownloadStatus.STATUS_CANCELED:
                synchronized (mOnDownloadListener) {
                    mStatus = DownloadStatus.STATUS_CANCELED;
                    mOnDownloadListener.onDownloadCanceled();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown state");
        }
    }


    private void executeDownload() throws DownloadException {
        final URL url;
        try {
            url = new URL(mThreadInfo.getUri());
        } catch (MalformedURLException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.", e);
        }

        HttpURLConnection httpConnection = null;
        try {
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(Constants.HTTP.CONNECT_TIME_OUT);
            httpConnection.setReadTimeout(Constants.HTTP.READ_TIME_OUT);
            httpConnection.setRequestMethod(Constants.HTTP.GET);
            setHttpHeader(getHttpHeaders(mThreadInfo), httpConnection);
            final int responseCode = httpConnection.getResponseCode();
            if (responseCode == getResponseCode()) {
                transferData(httpConnection);
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

//    private void executeDownload() throws DownloadException {
//
//        OkHttpClient okHttpClient = OkhttpUtil.getOkHttpClient();
//
//
//        Request.Builder builder = new Request.Builder().url(mThreadInfo.getUri());
//
//        setHttpHeader(getHttpHeaders(mThreadInfo),builder);
//
//
//        Request request = builder.build();
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
//
//            if (responseCode == getResponseCode()) {
//                transferData(response);
//            } else {
//                throw new DownloadException(DownloadStatus.STATUS_FAILED, "UnSupported response code:" + responseCode);
//            }
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.", e);
//        }
//
//    }


//    private void setHttpHeader(Map<String, String> headers, Request.Builder builder) {
//        if (headers != null) {
//            for (String key : headers.keySet()) {
//                builder.addHeader(key, headers.get(key));
//            }
//        }
//    }


    private void setHttpHeader(Map<String, String> headers, URLConnection connection) {
        if (headers != null) {
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
        }
    }

    private void transferData(HttpURLConnection httpConnection) throws DownloadException {
        InputStream inputStream = null;
        RandomAccessFile raf = null;
        try {
            try {
                inputStream = httpConnection.getInputStream();
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "http get inputStream error", e);
            }
            final long offset = mThreadInfo.getStart() + mThreadInfo.getFinished();
            try {
                raf = getFile(mDownloadInfo.getDir(), mDownloadInfo.getName(), offset);
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "File error", e);
            }
            transferData(inputStream, raf);
        } finally {
            IOUtil.close(inputStream);
            IOUtil.close(raf);
        }
    }
//    private void transferData(Response response) throws DownloadException {
//        InputStream inputStream = null;
//        RandomAccessFile raf = null;
//        try {
//
//            inputStream = response.body().byteStream();
//
//            final long offset = mThreadInfo.getStart() + mThreadInfo.getFinished();
//            try {
//                raf = getFile(mDownloadInfo.getDir(), mDownloadInfo.getName(), offset);
//            } catch (IOException e) {
//                throw new DownloadException(DownloadStatus.STATUS_FAILED, "File error", e);
//            }
//            transferData(inputStream, raf);
//        } finally {
//            IOUtil.close(inputStream);
//            IOUtil.close(raf);
//        }
//    }


    private void transferData(InputStream inputStream, RandomAccessFile raf) throws DownloadException {
        final byte[] buffer = new byte[1024 * 16];
        while (true) {

            if (checkPausedOrCanceled())
                return;
            int len = -1;
            try {
                len = inputStream.read(buffer);
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "Http inputStream read error", e);
            }

            if (len == -1) {
                break;
            }

            try {
                raf.write(buffer, 0, len);
                mThreadInfo.setFinished(mThreadInfo.getFinished() + len);

                LogUtil.getInstance().d("filesize:" + raf.length() + ";" + mDownloadInfo.getName() + mThreadInfo.getId() + ":finished" + (mThreadInfo.getFinished() + len));


                synchronized (mOnDownloadListener) {
                    mDownloadInfo.setFinished(mDownloadInfo.getFinished() + len);
                    mOnDownloadListener.onDownloadProgress(mDownloadInfo.getFinished(), mDownloadInfo.getLength());
                }
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "Fail write buffer to file", e);
            }
        }
    }


    private boolean checkPausedOrCanceled() throws DownloadException {
        if (mCommend == DownloadStatus.STATUS_CANCELED) {
            // cancel
//            throw new DownloadException(DownloadStatus.STATUS_CANCELED, "Download canceled!");

            handleDownloadException(new DownloadException(DownloadStatus.STATUS_CANCELED, "Download canceled!"));

            return true;
        } else if (mCommend == DownloadStatus.STATUS_PAUSED) {

            LogUtil.getInstance().d("mCommend == DownloadStatus.STATUS_PAUSED");
            // pause
            updateDB(mThreadInfo);
//            throw new DownloadException(DownloadStatus.STATUS_PAUSED, "Download paused!");

//            handleDownloadException(new DownloadException(DownloadStatus.STATUS_PAUSED, "Download paused!"));

            handleDownloadException(new DownloadException(DownloadStatus.STATUS_PAUSED, "Download paused!"));
            return true;
        }

        return false;


    }


    protected abstract void insertIntoDB(ThreadInfo info);

    protected abstract int getResponseCode();

    protected abstract void updateDB(ThreadInfo info);

    protected abstract Map<String, String> getHttpHeaders(ThreadInfo info);

    protected abstract RandomAccessFile getFile(File dir, String name, long offset) throws IOException;

    protected abstract String getTag();
}
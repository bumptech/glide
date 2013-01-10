package com.bumptech.flickr.api;

import android.os.Handler;
import android.os.HandlerThread;
import com.bumptech.photos.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/6/13
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class Downloader {
    private static Downloader DOWNLOADER;
    private final Handler mainHandler;
    private final ExecutorService executor;

    static Downloader get() {
        if (DOWNLOADER == null) {
            DOWNLOADER = new Downloader();
        }
        return DOWNLOADER;
    }

    public interface MemoryCallback {
        public void onDownloadReady(byte[] data);
    }

    public interface DiskCallback {
        public void onDownloadReady(String path);
    }

    protected Downloader() {
        HandlerThread workerThread = new HandlerThread("downloader_thread");
        workerThread.start();
        executor = Executors.newFixedThreadPool(6);
        mainHandler = new Handler();
    }

    private void post(Runnable runnable) {
        executor.execute(runnable);
    }

    public void download(String url, MemoryCallback cb) {
        post(new DownloadWorker(url, cb));
    }

    public void download(String url, File out, DiskCallback cb) {
        post(new DiskDownloadWorker(url, out, cb));
    }

    private class DiskDownloadWorker implements Runnable {
        private final String url;
        private final DiskCallback cb;
        private final File output;

        public DiskDownloadWorker(String url, File output, DiskCallback cb) {
            this.url = url;
            this.output = output;
            this.cb = cb;
        }

        @Override
        public void run() {
            Log.d("Downloader: run");
            HttpURLConnection urlConnection = null;
            try {
                final URL targetUrl = new URL(url);
                urlConnection = (HttpURLConnection) targetUrl.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                OutputStream out = new FileOutputStream(output);
                writeToOutput(in, out);
                out.close();
                in.close();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onDownloadReady(output.getPath());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        private void writeToOutput(InputStream in, OutputStream out) throws IOException {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (((bytesRead = in.read(buffer)) != -1)) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private class DownloadWorker implements Runnable {

        private final String url;
        private final MemoryCallback cb;

        public DownloadWorker(String url, MemoryCallback cb) {
            this.url = url;
            this.cb = cb;
        }

        @Override
        public void run() {
            HttpURLConnection urlConnection = null;
            try {
                final URL targetUrl = new URL(url);
                urlConnection = (HttpURLConnection) targetUrl.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                final List<Byte> data = new ArrayList<Byte>(1024);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while (((bytesRead = in.read(buffer)) != -1)) {
                    for (int i = 0; i < bytesRead; i++) {
                        data.add(buffer[i]);
                    }
                }
                final byte[] result = new byte[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    result[i] = data.get(i);
                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onDownloadReady(result);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
    }
}

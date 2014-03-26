package com.bumptech.glide.resize;

import android.annotation.TargetApi;
import android.os.Build;
import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

public class SafeKeyGenerator {
    private final LruCache<LoadId, String> loadIdToSafeHash = new LruCache<LoadId, String>(250);
    private final LoadIdPool loadIdPool = new LoadIdPool();

    public String getSafeKey(BitmapLoad task) {
        LoadId loadId = loadIdPool.get(task.getId());
        String safeKey = loadIdToSafeHash.get(loadId);
        if (safeKey == null) {
            try {
                safeKey = loadId.generateSafeKey();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            loadIdToSafeHash.put(loadId, safeKey);
        } else {
            loadIdPool.offer(loadId);
        }
        return safeKey;
    }

    private class LoadIdPool {
        private static final int MAX_SIZE = 20;
        private Queue<LoadId> loadIdQueue;

        @TargetApi(9)
        public LoadIdPool() {
            if (Build.VERSION.SDK_INT >= 9) {
                loadIdQueue = new ArrayDeque<LoadId>(MAX_SIZE);
            } else {
                loadIdQueue = new LinkedList<LoadId>();
            }
        }

        public LoadId get(String id) {
            LoadId loadId = loadIdQueue.poll();
            if (loadId == null) {
                loadId = new LoadId();
            }
            loadId.init(id);
            return loadId;
        }

        public void offer(LoadId loadId) {
            if (loadIdQueue.size() < MAX_SIZE) {
                loadIdQueue.offer(loadId);
            }
        }
    }

    private class LoadId {
        private String id;

        public void init(String id) {
            this.id = id;
        }

        public String generateSafeKey() throws UnsupportedEncodingException, NoSuchAlgorithmException {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(id.getBytes("UTF-8"));
            return Util.sha256BytesToHex(messageDigest.digest());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LoadId loadId = (LoadId) o;

            if (!id.equals(loadId.id)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}

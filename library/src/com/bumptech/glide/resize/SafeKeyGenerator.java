package com.bumptech.glide.resize;

import android.os.Build;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.Transformation;
import com.bumptech.glide.util.Util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SafeKeyGenerator {
    private final Map<LoadId, String> loadIdToSafeHash = new HashMap<LoadId, String>();
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
    private final LoadIdPool loadIdPool = new LoadIdPool();
    private MessageDigest messageDigest;

    public SafeKeyGenerator() {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String getSafeKey(String id, Transformation transformation, Downsampler downsampler, int width, int height) {
        LoadId loadId = loadIdPool.get(id, transformation.getId(), downsampler.getId(), width, height);
        String safeKey = loadIdToSafeHash.get(loadId);
        if (safeKey == null) {
            try {
                safeKey = loadId.generateSafeKey();
            } catch (UnsupportedEncodingException e) {
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

        public LoadIdPool() {
            if (Build.VERSION.SDK_INT >= 9) {
                loadIdQueue = new ArrayDeque<LoadId>(MAX_SIZE);
            } else {
                loadIdQueue = new LinkedList<LoadId>();
            }
        }

        public LoadId get(String id, String transformationId, String downsamplerId, int width, int height) {
            LoadId loadId = loadIdQueue.poll();
            if (loadId == null) {
                loadId = new LoadId();
            }
            loadId.init(id, transformationId, downsamplerId, width, height);
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
        private String transformationId;
        private String downsamplerId;
        private int width;
        private int height;

        public void init(String id, String transformationId, String downsamplerId, int width, int height) {
            this.id = id;
            this.transformationId = transformationId;
            this.downsamplerId = downsamplerId;
            this.width = width;
            this.height = height;
        }

        public String generateSafeKey() throws UnsupportedEncodingException {
            messageDigest.update(id.getBytes("UTF-8"));
            messageDigest.update(transformationId.getBytes("UTF-8"));
            messageDigest.update(downsamplerId.getBytes("UTF-8"));
            byteBuffer.position(0);
            byteBuffer.putInt(width);
            byteBuffer.putInt(height);
            messageDigest.update(byteBuffer.array());
            return Util.sha256BytesToHex(messageDigest.digest());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LoadId loadId = (LoadId) o;

            if (height != loadId.height) return false;
            if (width != loadId.width) return false;
            if (!downsamplerId.equals(loadId.downsamplerId)) return false;
            if (!id.equals(loadId.id)) return false;
            if (!transformationId.equals(loadId.transformationId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + transformationId.hashCode();
            result = 31 * result + downsamplerId.hashCode();
            result = 31 * result + width;
            result = 31 * result + height;
            return result;
        }
    }
}

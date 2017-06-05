package com.bumptech.glide.samples.flickr;

import android.util.Log;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.Entity;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.facebook.crypto.keychain.KeyChain;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import hugo.weaving.DebugLog;

@DebugLog
public class ConcealFileEncoder implements ResourceEncoder<byte[]> {

    private static final String TAG = "ConcealFileEncoder";

    @Override
    public boolean encode(Resource<byte[]> resource, File file, Options options) {
        final byte[] bytesArray = resource.get();

        boolean success = false;
        OutputStream outputStream = null;
        try {

            KeyChain keyChain = new SharedPrefsBackedKeyChain(App.getInstance(), CryptoConfig.KEY_256);
            Crypto crypto = AndroidConceal.get().createDefaultCrypto(keyChain);
            if (!crypto.isAvailable()) {
                throw new RuntimeException();
            }
            OutputStream fileStream = new BufferedOutputStream(new FileOutputStream(file));
            outputStream = crypto.getCipherOutputStream(fileStream, Entity.create(file.getName()));
            outputStream.write(bytesArray);
            outputStream.close();
            success = true;
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to encode Bitmap", e);
            }
        } catch (KeyChainException | CryptoInitializationException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to encrypt Bitmap", e);
                throw new RuntimeException(e);
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
        }


        return success;
    }


    @Override
    public EncodeStrategy getEncodeStrategy(Options options) {
        return EncodeStrategy.TRANSFORMED;
    }
}

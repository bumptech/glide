package com.bumptech.glide.samples.flickr;


import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bytes.BytesResource;
import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.Entity;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.facebook.crypto.keychain.KeyChain;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import hugo.weaving.DebugLog;

@DebugLog
public class ConcealFileDecoder implements ResourceDecoder<File, byte[]> {

    @Override
    public boolean handles(File source, Options options) {
        return true;
    }

    @Override
    public Resource<byte[]> decode(File source, int width, int height, Options options) {

        KeyChain keyChain = new SharedPrefsBackedKeyChain(App.getInstance(), CryptoConfig.KEY_256);
        Crypto crypto = AndroidConceal.get().createDefaultCrypto(keyChain);

        if (!crypto.isAvailable()) {
            throw new RuntimeException();
        }

        FileInputStream fileStream;
        try {
            fileStream = new FileInputStream(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int read;
            byte[] buffer = new byte[1024];
            InputStream macInputStream = crypto.getCipherInputStream(fileStream, Entity.create(source.getAbsolutePath()));

            while ((read = macInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.close();
            macInputStream.close();
            return new BytesResource(out.toByteArray());

        } catch (KeyChainException | IOException | CryptoInitializationException e) {
            throw new RuntimeException(e);

        }

    }
}

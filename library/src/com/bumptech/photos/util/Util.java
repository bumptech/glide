package com.bumptech.photos.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/20/13
 * Time: 9:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class Util {
    private static final int PRIME = 31;

    public static int hash(int... hashes) {
        int result = 1;
        for (int hash : hashes) {
            result *= PRIME * hash;
        }
        return result;
    }

    public static String sha1Hash(String toHash) {
        String hash = null;
        try {
            byte[] bytes = toHash.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(bytes, 0, bytes.length);
            hash = new BigInteger(1, digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }
}

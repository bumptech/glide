package com.bumptech.glide.util;

public class Util {
    private static final int PRIME = 31;

    public static int hash(int... hashes) {
        int result = 1;
        for (int hash : hashes) {
            result *= PRIME * hash;
        }
        return result;
    }
}

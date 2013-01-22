package com.bumptech.photos.util;

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
}

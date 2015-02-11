package com.bumptech.glide.load;

/**
 * Indicates the origin of some retrieved data.
 */
public enum DataSource {
    /**
     * Indicates this data was probably retrieved locally from the device, although it may have been obtained
     * through a content provider that may have obtained the data from a remote source.
     */
    LOCAL,
    /**
     * Indicates this data was retrieved from a remote source other than the device.
     */
    REMOTE,
}

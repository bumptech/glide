package com.bumptech.swatchsample.app;

import android.net.Uri;

import java.util.Locale;
import java.util.Random;

/**
 * Generates random {@link android.net.Uri}s from <a href="http://lorempixel.com">http://lorempixel.com</a>.
 */
class LoremPixelUrlGenerator {
    private static final String[] CATEGORIES = {
            "abstract", "animals", "business", "cats", "city", "food", "nightlife",
            "fashion", "people", "nature", "sports", "technics", "transport"
    };
    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 400;
    public static final int MAX_VALID_INDEX = 10;

    private final Random random = new Random(0);

    public Uri[] generateAll() {
        Uri[] urls = new Uri[CATEGORIES.length * MAX_VALID_INDEX];
        for (int c = 0; c < CATEGORIES.length; c++) {
            for (int i = 0; i < MAX_VALID_INDEX; i++) {
                urls[c * MAX_VALID_INDEX + i] = getUrl(DEFAULT_WIDTH, DEFAULT_HEIGHT, CATEGORIES[c], i + 1);
            }
        }
        return urls;
    }

    public Uri[] generate(int size) {
        Uri[] urls = new Uri[size];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = generate();
        }
        return urls;
    }

    public Uri generate() {
        int c = random.nextInt(CATEGORIES.length);
        int i = random.nextInt(MAX_VALID_INDEX) + 1;
        return getUrl(DEFAULT_WIDTH, DEFAULT_HEIGHT, CATEGORIES[c], i);
    }

    public static Uri getUrl(int w, int h, String category, int index) {
        String uri = String.format(Locale.ROOT, "http://lorempixel.com/%d/%d/%s/%d", w, h, category, index);
        return Uri.parse(uri);
    }
}

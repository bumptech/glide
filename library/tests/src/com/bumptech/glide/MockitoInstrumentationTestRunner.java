package com.bumptech.glide;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;

public class MockitoInstrumentationTestRunner extends InstrumentationTestRunner {
    @Override
    public void onCreate(Bundle arguments) {
        // See https://code.google.com/p/dexmaker/issues/detail?id=2.
        System.setProperty("dexmaker.dexcache", getTargetContext().getCacheDir().getPath());
        super.onCreate(arguments);
    }
}

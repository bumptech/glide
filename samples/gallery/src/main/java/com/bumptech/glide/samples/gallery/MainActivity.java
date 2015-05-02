package com.bumptech.glide.samples.gallery;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;

/**
 * Displays a {@link com.bumptech.glide.samples.gallery.RecyclerViewFragment}.
 */
public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);
        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH);

        if (savedInstanceState == null) {
            RecyclerViewFragment fragment = new RecyclerViewFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}

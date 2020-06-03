package com.bumptech.glide.test;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.Glide;

public class GlideWithBeforeSuperOnCreateActivity extends FragmentActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Glide.with(this);
    super.onCreate(savedInstanceState);
    setContentView(new TextView(this));
  }

  @Override
  protected void onResume() {
    super.onResume();
    Glide.with(this);
  }
}

package com.bumptech.glide.test;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.Glide;

public class GlideWithAsDifferentSupertypesActivity extends FragmentActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Glide.with(this);
    Glide.with((Context) this);
    Glide.with((Activity) this);
  }
}

package com.bumptech.glide.test;

import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.bumptech.glide.instrumentation.R;
import com.bumptech.glide.util.Preconditions;

public class ForceDarkOrLightModeActivity extends AppCompatActivity {
  private static final int INVALID_MODE = -1;
  private static final String ARGS_NIGHT_MODE = "args_night_mode";

  public static Intent forceLightMode(Context context) {
    return newArgs(context, AppCompatDelegate.MODE_NIGHT_NO);
  }

  public static Intent forceDarkMode(Context context) {
    return newArgs(context, AppCompatDelegate.MODE_NIGHT_YES);
  }

  private static Intent newArgs(Context context, int nightMode) {
    Intent intent = new Intent(context, ForceDarkOrLightModeActivity.class);
    intent.putExtra(ARGS_NIGHT_MODE, nightMode);
    return intent;
  }

  @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR1)
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    int modeToForce = getIntent().getExtras().getInt(ARGS_NIGHT_MODE, INVALID_MODE);
    Preconditions.checkArgument(modeToForce != INVALID_MODE, "Invalid mode: " + modeToForce);
    getDelegate().setLocalNightMode(modeToForce);
    setContentView(R.layout.default_fragment_activity);
  }
}

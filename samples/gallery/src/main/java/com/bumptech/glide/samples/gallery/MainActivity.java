package com.bumptech.glide.samples.gallery;

import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.MemoryCategory;

/** Displays a {@link HorizontalGalleryFragment}. */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends FragmentActivity {

  private static final int REQUEST_READ_STORAGE = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    GlideApp.get(this).setMemoryCategory(MemoryCategory.HIGH);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
      requestStoragePermission();
    } else {
      replaceFragment();
    }
  }

  private void requestStoragePermission() {
    ActivityCompat.requestPermissions(
        this, new String[] {permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
  }

  private void replaceFragment() {
    Fragment fragment = new HorizontalGalleryFragment();
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .commit();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case REQUEST_READ_STORAGE:
        {
          // If request is cancelled, the result arrays are empty.
          if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            replaceFragment();
          } else {
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show();
            requestStoragePermission();
          }
        }
    }
  }
}

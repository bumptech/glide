package com.bumptech.glide.manager;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.bumptech.glide.Glide;

/** A test activity to reproduce Issue #117: https://github.com/bumptech/glide/issues/117. */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class Issue117Activity extends FragmentActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ViewPager viewPager = new ViewPager(this);
    viewPager.setId(View.generateViewId());
    setContentView(viewPager, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    viewPager.setAdapter(new Issue117Adapter(getSupportFragmentManager()));
  }

  private static class Issue117Adapter extends FragmentPagerAdapter {

    Issue117Adapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      return new Issue117Fragment();
    }

    @Override
    public int getCount() {
      return 1;
    }
  }

  public static class Issue117Fragment extends Fragment {
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      return new Issue117ImageView(getActivity());
    }
  }

  public static class Issue117ImageView extends ImageView {
    public Issue117ImageView(Context context) {
      super(context);
    }

    @Override
    protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      Glide.with(getContext()).asDrawable().load(android.R.drawable.ic_menu_rotate).into(this);
    }
  }
}

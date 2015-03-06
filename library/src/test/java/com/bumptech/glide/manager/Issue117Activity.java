package com.bumptech.glide.manager;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

/**
 * A test activity to reproduce Issue #117: https://github.com/bumptech/glide/issues/117.
 */
class Issue117Activity extends FragmentActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ViewPager viewPager = new ViewPager(this);
    viewPager.setId(View.generateViewId());
    setContentView(viewPager, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));
    viewPager.setAdapter(new Issue117Adapter(getSupportFragmentManager()));
  }

  private static class Issue117Adapter extends FragmentPagerAdapter {

    public Issue117Adapter(FragmentManager fm) {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
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


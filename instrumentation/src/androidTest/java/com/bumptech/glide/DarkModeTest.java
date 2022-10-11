package com.bumptech.glide;

import static androidx.test.espresso.Espresso.onIdle;
import static com.bumptech.glide.testutil.BitmapSubject.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.instrumentation.R;
import com.bumptech.glide.load.engine.executor.IdlingGlideRule;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.ForceDarkOrLightModeActivity;
import com.google.common.base.Function;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DarkModeTest {
  private final Context context = ApplicationProvider.getApplicationContext();
  @Rule public final IdlingGlideRule idlingGlideRule =
      IdlingGlideRule.newGlideRule(glideBuilder -> glideBuilder);

  // TODO(judds): The way we handle data loads in the background for resoures is not Theme
  //  compatible. In particular, the theme gets lost when we convert the resource id to a Uri and
  // we don't use the user provided theme. While ResourceBitmapDecoder and ResourceDrawableDecoder
  // will use the theme, they're not called for most resource ids because those instead go through
  // UriLoader, which just calls contentResolver.openStream. This isn't sufficient to use to theme.
  // We could:
  // 1. Avoid using contentResolver for android resource Uris and use ResourceBitmapDecoder instead.
  // 2. #1 but only for non-raw resources which won't be themed
  // 3. Always use Theme.getResources().openRawResource, which, despite the name, works find on
  // Drawables and takes into account the theme.
  // In addition we'd also need to consider just passing through the theme always, rather than only
  // when it's specified by the user. Otherwise whether or not we'd obey dark mode would depend on
  // the user also providing the theme from the activity. We'd want to try to make sure that doesn't
  // leak the Activity.
  // TODO(judds): Add tests for Fragments for load().
  @Test
  public void load_withDarkModeActivity_usesLightModeDrawable() {
      runActivityTest(
          darkModeActivity(),
          R.raw.dog_light,
          activity -> Glide.with(activity).load(R.drawable.dog).override(Target.SIZE_ORIGINAL));
  }

  @Test
  public void load_withDarkModeFragment_usesLightModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_light,
        fragment -> Glide.with(fragment).load(R.drawable.dog).override(Target.SIZE_ORIGINAL));
  }

  @Test
  public void load_withLightModeActivity_usesLightModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        activity -> Glide.with(activity).load(R.drawable.dog).override(Target.SIZE_ORIGINAL));
  }

  @Test
  public void load_withLightModeFragment_usesLightModeDrawable() {
    runFragmentTest(
        lightModeActivity(),
        R.raw.dog_light,
        fragment -> Glide.with(fragment).load(R.drawable.dog).override(Target.SIZE_ORIGINAL));
  }

  @Ignore("We do not asynchronously load resources correctly")
  @Test
  public void load_withDarkModeActivity_darkModeTheme_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity ->
            Glide.with(activity)
                .load(R.drawable.dog)
                .override(Target.SIZE_ORIGINAL)
                .theme(activity.getTheme()));
  }

  @Ignore("We do not asynchronously load resources correctly")
  @Test
  public void load_withDarkModeFragment_darkModeTheme_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        fragment ->
            Glide.with(fragment)
                .load(R.drawable.dog)
                .override(Target.SIZE_ORIGINAL)
                .theme(fragment.requireActivity().getTheme()));
  }

  @Test
  public void load_withLightModeActivity_lightModeTheme_usesLightModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        activity ->
            Glide.with(activity)
                .load(R.drawable.dog)
                .override(Target.SIZE_ORIGINAL)
                .theme(activity.getTheme()));
  }

  @Test
  public void placeholder_withDarkModeActivity_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input)
                .load((Object) null)
                .placeholder(R.drawable.dog));
  }

  @Test
  public void placeholder_withDarkModeFragment_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input)
                .load((Object) null)
                .placeholder(R.drawable.dog));
  }

  @Test
  public void error_withDarkModeActivity_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input)
                .load((Object) null)
                .error(R.drawable.dog));
  }

  @Test
  public void error_withDarkModeFragment_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input)
                .load((Object) null)
                .error(R.drawable.dog));
  }

  @Test
  public void fallback_withDarkModeActivity_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input)
                .load((Object) null)
                .fallback(R.drawable.dog));
  }

  @Test
  public void fallback_withDarkModeFragment_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input)
                .load((Object) null)
                .fallback(R.drawable.dog));
  }


  @Test
  public void placeholder_withLightModeActivity_usesLightModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        input ->
            Glide.with(input)
                .load((Object) null)
                .placeholder(R.drawable.dog));
  }

  @Test
  public void placeholder_withLightModeFragment_usesLightModeDrawable() {
    runFragmentTest(
        lightModeActivity(),
        R.raw.dog_light,
        input ->
            Glide.with(input)
                .load((Object) null)
                .placeholder(R.drawable.dog));
  }

  @Test
  public void placeholder_withDarkModeActivityAndTheme_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input)
            .load((Object) null)
            .theme(input.getTheme())
            .placeholder(R.drawable.dog));
  }

  @Test
  public void placeholder_withLightModeActivityAndTheme_usesLightModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        input ->
            Glide.with(input)
                .load((Object) null)
                .theme(input.getTheme())
                .placeholder(R.drawable.dog));
  }

  @Test
  public void placeholder_withApplicationContext_darkTheme_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input.getApplicationContext())
                .load((Object) null)
                .theme(input.getTheme())
                .placeholder(R.drawable.dog));
  }

  @Test
  public void placeholder_withApplicationContext_lightTheme_usesLightModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        input ->
            Glide.with(input.getApplicationContext())
                .load((Object) null)
                .theme(input.getTheme())
                .placeholder(R.drawable.dog));
  }

  private ActivityScenario<FragmentActivity> darkModeActivity() {
    return ActivityScenario.launch(ForceDarkOrLightModeActivity.forceDarkMode(context));
  }

  private ActivityScenario<FragmentActivity> lightModeActivity() {
    return ActivityScenario.launch(ForceDarkOrLightModeActivity.forceLightMode(context));
  }

  private static void runFragmentTest(
      ActivityScenario<? extends FragmentActivity> scenario,
      int expectedResource,
      Function<Fragment, RequestBuilder<Drawable>> requestBuilder) {
    try (scenario) {
      scenario.onActivity(
          activity -> {
            ImageViewFragment fragment = new ImageViewFragment();
            activity
                .getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.container, fragment)
                .commitNowAllowingStateLoss();
            ViewGroup container = findContainer(activity);
            ImageView imageView = (ImageView) container.getChildAt(0);

            requestBuilder.apply(fragment).into(imageView);
          });

      assertImageViewContainerChildHasContent(scenario, expectedResource);
    }
  }

  public static final class ImageViewFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
      return newFixedSizeImageView(getContext());
    }
  }

  private static ImageView newFixedSizeImageView(Context context) {
    ImageView imageView = new ImageView(context);
    imageView.setLayoutParams(new LayoutParams(200, 200));
    return imageView;
  }

  private static void runActivityTest(
      ActivityScenario<? extends FragmentActivity> scenario,
      int expectedResource,
      Function<FragmentActivity, RequestBuilder<Drawable>> glideBuilder) {
    try (scenario) {
      scenario.onActivity(
          activity -> {
            ViewGroup container = findContainer(activity);
            ImageView imageView = newFixedSizeImageView(activity);
            container.addView(imageView);

            glideBuilder.apply(activity).into(imageView);
          });

      assertImageViewContainerChildHasContent(scenario, expectedResource);
    }
  }

  private static void assertImageViewContainerChildHasContent(
      ActivityScenario<? extends FragmentActivity> scenario, int expectedResource) {
    onIdle();
    scenario.onActivity(
        activity -> {
          ViewGroup container = findContainer(activity);
          ImageView imageView = (ImageView) container.getChildAt(0);
          Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
          assertThat(bitmap).sameAs(expectedResource);
        });
  }

  private static ViewGroup findContainer(FragmentActivity activity) {
    return activity.findViewById(R.id.container);
  }
}

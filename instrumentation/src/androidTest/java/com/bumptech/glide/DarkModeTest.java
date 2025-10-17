package com.bumptech.glide;

import static androidx.test.espresso.Espresso.onIdle;
import static com.bumptech.glide.testutil.BitmapSubject.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DarkModeTest {
  private final Context context = ApplicationProvider.getApplicationContext();

  @Rule
  public final IdlingGlideRule idlingGlideRule =
      IdlingGlideRule.newGlideRule(glideBuilder -> glideBuilder);

  @Before
  public void before() {
    // Dark mode wasn't supported prior to Q.
    assumeTrue(VERSION.SDK_INT >= VERSION_CODES.Q);
  }

  @Test
  public void load_withDarkModeActivity_vectorDrawable_usesDarkModeColor() {
    runActivityDrawableTest(
        darkModeActivity(),
        R.drawable.vector_drawable_dark,
        activity ->
            Glide.with(activity).load(R.drawable.vector_drawable).override(Target.SIZE_ORIGINAL));
  }

  @Test
  public void load_withLightModeActivity_vectorDrawable_usesLightModeColor() {
    runActivityDrawableTest(
        lightModeActivity(),
        R.drawable.vector_drawable_light,
        activity ->
            Glide.with(activity).load(R.drawable.vector_drawable).override(Target.SIZE_ORIGINAL));
  }

  private void runActivityDrawableTest(
      ActivityScenario<? extends FragmentActivity> scenario,
      int expectedResource,
      Function<FragmentActivity, RequestBuilder<Drawable>> glideBuilder) {
    AtomicReference<Bitmap> result = new AtomicReference<>();
    try (scenario) {
      scenario.onActivity(
          activity -> {
            ViewGroup container = findContainer(activity);
            ImageView imageView = newFixedSizeImageView(activity);
            container.addView(imageView);

            glideBuilder.apply(activity).into(imageView);
          });

      // This two step process is because setting the Drawable on the ImageView modifies the
      // drawable in a subsequent frame. If we want our Drawables to produce identical Bitmaps when
      // drawn to a canvas, we need to set both on the ImageView for at least one frame.
      onIdle();
      scenario.onActivity(
          activity -> {
            ImageView imageView = findImageView(activity);
            result.set(drawableToBitmap(imageView.getDrawable()));
            Drawable expectedDrawable = AppCompatResources.getDrawable(activity, expectedResource);
            imageView.setImageDrawable(expectedDrawable);
          });
      onIdle();
      scenario.onActivity(
          activity -> {
            ImageView imageView = findImageView(activity);
            Bitmap expected = drawableToBitmap(imageView.getDrawable());
            assertThat(result.get()).sameAs(expected);
          });
    }
  }

  private static Bitmap drawableToBitmap(Drawable drawable) {
    int width = drawable.getIntrinsicWidth();
    int height = drawable.getIntrinsicHeight();

    Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(result);
    drawable.setBounds(0, 0, width, height);
    drawable.draw(canvas);
    canvas.setBitmap(null);
    return result;
  }

  @Test
  public void load_withDarkModeActivity_useDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity -> Glide.with(activity).load(R.drawable.dog).override(Target.SIZE_ORIGINAL));
  }

  @Test
  public void load_withDarkModeActivity_afterLoadingWithLightModeActivity_useDarkModeDrawable() {
    // Load with light mode first.
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        activity -> Glide.with(activity).load(R.drawable.dog).override(Target.SIZE_ORIGINAL));

    // Then again with dark mode to make sure that we do not use the cached resource from the
    // previous load.
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity -> Glide.with(activity).load(R.drawable.dog).override(Target.SIZE_ORIGINAL));
  }

  @Test
  public void
      load_withDarkModeActivity_afterLoadingWithLightModeActivity_memoryCacheCleared_useDarkModeDrawable() {
    // Load with light mode first.
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        activity -> Glide.with(activity).load(R.drawable.dog).override(Target.SIZE_ORIGINAL));

    // Then again with dark mode to make sure that we do not use the cached resource from the
    // previous load.
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity -> {
          Glide.get(context).clearMemory();
          return Glide.with(activity).load(R.drawable.dog).override(Target.SIZE_ORIGINAL);
        });
  }

  @Test
  public void load_withDarkModeFragment_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
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

  @Test
  public void loadResourceNameUri_withDarkModeActivity_darkModeTheme_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity ->
            Glide.with(activity)
                .load(newResourceNameUri(activity, R.drawable.dog))
                .override(Target.SIZE_ORIGINAL)
                .theme(activity.getTheme()));
  }

  @Test
  public void loadResourceNameUri_withDarkModeActivity_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity ->
            Glide.with(activity)
                .load(newResourceNameUri(activity, R.drawable.dog))
                .override(Target.SIZE_ORIGINAL));
  }

  @Test
  public void
      loadResourceNameUri_withDarkModeActivity_afterLightModeActivity_usesDarkModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        activity ->
            Glide.with(activity)
                .load(newResourceNameUri(activity, R.drawable.dog))
                .override(Target.SIZE_ORIGINAL));
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity ->
            Glide.with(activity)
                .load(newResourceNameUri(activity, R.drawable.dog))
                .override(Target.SIZE_ORIGINAL));
  }

  @Test
  public void loadResourceIdUri_withDarkModeActivity_darkModeTheme_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity ->
            Glide.with(activity)
                .load(newResourceIdUri(activity, R.drawable.dog))
                .override(Target.SIZE_ORIGINAL)
                .theme(activity.getTheme()));
  }

  @Test
  public void loadResourceIdUri_withDarkModeActivity_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        activity ->
            Glide.with(activity)
                .load(newResourceIdUri(activity, R.drawable.dog))
                .override(Target.SIZE_ORIGINAL));
  }

  private static Uri newResourceNameUri(Context context, int resourceId) {
    Resources resources = context.getResources();
    return newResourceUriBuilder(context)
        .appendPath(resources.getResourceTypeName(resourceId))
        .appendPath(resources.getResourceEntryName(resourceId))
        .build();
  }

  private static Uri newResourceIdUri(Context context, int resourceId) {
    return newResourceUriBuilder(context).appendPath(String.valueOf(resourceId)).build();
  }

  private static Uri.Builder newResourceUriBuilder(Context context) {
    return new Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.getPackageName());
  }

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
  public void loadResourceNameUri_withDarkModeFragment_darkModeTheme_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        fragment ->
            Glide.with(fragment)
                .load(newResourceNameUri(fragment.requireContext(), R.drawable.dog))
                .override(Target.SIZE_ORIGINAL)
                .theme(fragment.requireActivity().getTheme()));
  }

  @Test
  public void loadResourceIdUri_withDarkModeFragment_darkModeTheme_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        fragment ->
            Glide.with(fragment)
                .load(newResourceIdUri(fragment.requireContext(), R.drawable.dog))
                .override(Target.SIZE_ORIGINAL)
                .theme(fragment.requireActivity().getTheme()));
  }

  @Test
  public void load_withApplicationContext_darkTheme_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input.getApplicationContext())
                .load(R.drawable.dog)
                .override(Target.SIZE_ORIGINAL)
                .theme(input.getTheme()));
  }

  @Ignore("TODO(#3751): Consider how to deal with themes applied for application context loads.")
  @Test
  public void load_withApplicationContext_lightTheme_thenDarkTheme_usesDarkModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        input ->
            Glide.with(input.getApplicationContext())
                .load(R.drawable.dog)
                .override(Target.SIZE_ORIGINAL)
                .theme(input.getTheme()));

    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input.getApplicationContext())
                .load(R.drawable.dog)
                .override(Target.SIZE_ORIGINAL)
                .theme(input.getTheme()));
  }

  @Test
  public void loadResourceNameUri_withApplicationContext_darkTheme_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input.getApplicationContext())
                .load(newResourceNameUri(input.getApplicationContext(), R.drawable.dog))
                .override(Target.SIZE_ORIGINAL)
                .theme(input.getTheme()));
  }

  @Ignore("TODO(#3751): Consider how to deal with themes applied for application context loads.")
  @Test
  public void
      loadResourceNameUri_withApplicationContext_darkTheme_afterLightTheme_usesDarkModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        input ->
            Glide.with(input.getApplicationContext())
                .load(newResourceNameUri(input.getApplicationContext(), R.drawable.dog))
                .override(Target.SIZE_ORIGINAL)
                .theme(input.getTheme()));

    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input.getApplicationContext())
                .load(newResourceNameUri(input.getApplicationContext(), R.drawable.dog))
                .override(Target.SIZE_ORIGINAL)
                .theme(input.getTheme()));
  }

  @Test
  public void loadResourceIdUri_withApplicationContext_darkTheme_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input ->
            Glide.with(input.getApplicationContext())
                .load(newResourceIdUri(input.getApplicationContext(), R.drawable.dog))
                .override(Target.SIZE_ORIGINAL)
                .theme(input.getTheme()));
  }

  @Test
  public void load_withApplicationContext_lightTheme_usesLightModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        input ->
            Glide.with(input.getApplicationContext())
                .load(R.drawable.dog)
                .override(Target.SIZE_ORIGINAL)
                .theme(input.getTheme()));
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
        input -> Glide.with(input).load((Object) null).placeholder(R.drawable.dog));
  }

  @Test
  public void placeholder_withDarkModeFragment_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input -> Glide.with(input).load((Object) null).placeholder(R.drawable.dog));
  }

  @Test
  public void error_withDarkModeActivity_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input -> Glide.with(input).load((Object) null).error(R.drawable.dog));
  }

  @Test
  public void error_withDarkModeFragment_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input -> Glide.with(input).load((Object) null).error(R.drawable.dog));
  }

  @Test
  public void fallback_withDarkModeActivity_usesDarkModeDrawable() {
    runActivityTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input -> Glide.with(input).load((Object) null).fallback(R.drawable.dog));
  }

  @Test
  public void fallback_withDarkModeFragment_usesDarkModeDrawable() {
    runFragmentTest(
        darkModeActivity(),
        R.raw.dog_dark,
        input -> Glide.with(input).load((Object) null).fallback(R.drawable.dog));
  }

  @Test
  public void placeholder_withLightModeActivity_usesLightModeDrawable() {
    runActivityTest(
        lightModeActivity(),
        R.raw.dog_light,
        input -> Glide.with(input).load((Object) null).placeholder(R.drawable.dog));
  }

  @Test
  public void placeholder_withLightModeFragment_usesLightModeDrawable() {
    runFragmentTest(
        lightModeActivity(),
        R.raw.dog_light,
        input -> Glide.with(input).load((Object) null).placeholder(R.drawable.dog));
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

  /** Fragment that displays a single fixed size ImageView. */
  public static final class ImageViewFragment extends Fragment {
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
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
          ImageView imageView = findImageView(activity);
          Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
          assertThat(bitmap).sameAs(expectedResource);
        });
  }

  private static ImageView findImageView(FragmentActivity activity) {
    ViewGroup container = findContainer(activity);
    return (ImageView) container.getChildAt(0);
  }

  private static ViewGroup findContainer(FragmentActivity activity) {
    return activity.findViewById(R.id.container);
  }
}

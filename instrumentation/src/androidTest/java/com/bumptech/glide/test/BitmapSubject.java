package com.bumptech.glide.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.v4.content.res.ResourcesCompat;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

/**
 * Truth assertions for comparing {@link Bitmap}s.
 */
// Test APIs.
@SuppressWarnings({"WeakerAccess", "unused"})
public final class BitmapSubject extends Subject<BitmapSubject, Bitmap> {

  private static final Subject.Factory<BitmapSubject, Bitmap> FACTORY =
      new Subject.Factory<BitmapSubject, Bitmap>() {
        @Override
        public BitmapSubject createSubject(
            @NonNull FailureMetadata metadata, @NonNull Bitmap actual) {
          return new BitmapSubject(metadata, actual);
        }
      };

  private BitmapSubject(FailureMetadata failureMetadata, Bitmap subject) {
    super(failureMetadata, subject);
  }

  public static BitmapSubject assertThat(Drawable drawable) {
    if (!(drawable instanceof BitmapDrawable)) {
      throw new IllegalArgumentException("Not a BitmapDrawable: " + drawable);
    }
    return assertThat(((BitmapDrawable) drawable).getBitmap());
  }

  public static BitmapSubject assertThat(Bitmap bitmap) {
    return Truth.assertAbout(FACTORY).that(bitmap);
  }

  @Override
  protected String actualCustomStringRepresentation() {
    return getDisplayString(actual());
  }

  private static String getDisplayString(Bitmap bitmap) {
     return "<"
        + "[" + bitmap.getWidth() + "x" + bitmap.getHeight() + "]"
        + " "
        + bitmap.getConfig()
        + ">";
  }

  public void sameAs(@DrawableRes int resourceId) {
    Context context = InstrumentationRegistry.getTargetContext();
    Drawable drawable =
        ResourcesCompat.getDrawable(context.getResources(), resourceId, context.getTheme());
    sameAs(drawable);
  }

  public void hasDimensions(int expectedWidth, int expectedHeight) {
    int actualWidth = actual().getWidth();
    int actualHeight = actual().getHeight();
    String message;
    if (expectedWidth != actualWidth && expectedHeight != actualHeight) {
      message = "has dimensions of [" + expectedWidth + "x" + expectedHeight + "]";
    } else if (expectedWidth != actualWidth) {
      message = "has width of " + expectedWidth;
    } else if (expectedHeight != actualHeight) {
      message = "has height of " + expectedHeight;
    } else {
      message = null;
    }

    if (message != null) {
      fail(message);
    }
  }

  public void isMutable()  {
    if (!actual().isMutable()) {
      fail("is mutable");
    }
  }

  public void isImmutable() {
    if (actual().isMutable()) {
      fail("is immutable");
    }
  }

  public void isNotRecycled() {
    if (actual().isRecycled()) {
      fail("is not recycled");
    }
  }

  @SuppressWarnings({"unchecked", "ConstantConditions"})
  public void sameAs(Drawable other) {
    if (!(other instanceof BitmapDrawable)) {
      fail("Not a BitmapDrawable");
    }
    sameAs(((BitmapDrawable) other).getBitmap());
  }

  public void sameAs(Bitmap other) {
    if (!actual().sameAs(other)) {
      fail("is the same as " + getDisplayString(other));
    }
  }

  public void isNotSameAs(Bitmap other) {
    if (actual().sameAs(other)) {
      fail("is not the same as " + getDisplayString(other));
    }
  }
}

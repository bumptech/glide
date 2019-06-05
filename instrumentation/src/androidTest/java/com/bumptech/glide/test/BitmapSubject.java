package com.bumptech.glide.test;

import static com.google.common.truth.Fact.simpleFact;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.test.InstrumentationRegistry;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

/** Truth assertions for comparing {@link Bitmap}s. */
// Test APIs.
@SuppressWarnings({"WeakerAccess", "unused", "rawtypes", "unchecked"})
public final class BitmapSubject extends Subject {

  private static final Subject.Factory<BitmapSubject, Bitmap> FACTORY =
      new Subject.Factory<BitmapSubject, Bitmap>() {
        @Override
        public BitmapSubject createSubject(
            @NonNull FailureMetadata metadata, @NonNull Bitmap actual) {
          return new BitmapSubject(metadata, actual);
        }
      };

  private final Bitmap actual;

  private BitmapSubject(FailureMetadata failureMetadata, Bitmap subject) {
    super(failureMetadata, subject);
    this.actual = subject;
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
    return getDisplayString(actual);
  }

  private static String getDisplayString(Bitmap bitmap) {
    return "<"
        + "["
        + bitmap.getWidth()
        + "x"
        + bitmap.getHeight()
        + "]"
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
    int actualWidth = actual.getWidth();
    int actualHeight = actual.getHeight();
    if (expectedWidth != actualWidth && expectedHeight != actualHeight) {
      failWithActual("expected to have dimensions", expectedWidth + "x" + expectedHeight);
    } else if (expectedWidth != actualWidth) {
      failWithActual("expected to have width", expectedWidth);
    } else if (expectedHeight != actualHeight) {
      failWithActual("expected to have height", expectedHeight);
    }
  }

  public void isMutable() {
    if (!actual.isMutable()) {
      failWithActual(simpleFact("expected to be mutable"));
    }
  }

  public void isImmutable() {
    if (actual.isMutable()) {
      failWithActual(simpleFact("expected to be immutable"));
    }
  }

  public void isNotRecycled() {
    if (actual.isRecycled()) {
      failWithActual(simpleFact("expected not to be recycled"));
    }
  }

  @SuppressWarnings({"unchecked", "ConstantConditions"})
  public void sameAs(Drawable other) {
    if (!(other instanceof BitmapDrawable)) {
      failWithoutActual(simpleFact("The given expected value was not a BitmapDrawable."));
    }
    sameAs(((BitmapDrawable) other).getBitmap());
  }

  public void sameAs(Bitmap other) {
    if (!actual.sameAs(other)) {
      failWithActual("expected to be the same as", getDisplayString(other));
    }
  }

  public void isNotSameAs(Bitmap other) {
    if (actual.sameAs(other)) {
      failWithActual("expected not to be the same as", getDisplayString(other));
    }
  }
}

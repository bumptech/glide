package com.bumptech.glide;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.tests.TearDownGlide;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;

// This test is about edge cases that might otherwise make debugging more challenging.
@RunWith(AndroidJUnit4.class)
public class InitializeGlideTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final Context context = ApplicationProvider.getApplicationContext();

  private static final class TestException extends RuntimeException {
    private static final long serialVersionUID = 2515021766931124927L;
  }

  @Test
  public void initialize_whenInternalMethodThrows_throwsException() {
    assertThrows(
        TestException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            synchronized (Glide.class) {
              Glide.checkAndInitializeGlide(
                  context,
                  new GeneratedAppGlideModule() {
                    @NonNull
                    @Override
                    Set<Class<?>> getExcludedModuleClasses() {
                      throw new TestException();
                    }
                  });
            }
          }
        });
  }

  @Test
  public void initialize_whenInternalMethodThrows_andCalledTwice_throwsException() {
    GeneratedAppGlideModule throwingGeneratedAppGlideModule =
        new GeneratedAppGlideModule() {
          @NonNull
          @Override
          Set<Class<?>> getExcludedModuleClasses() {
            throw new TestException();
          }
        };
    ThrowingRunnable initializeGlide =
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            synchronized (Glide.class) {
              Glide.checkAndInitializeGlide(context, throwingGeneratedAppGlideModule);
            }
          }
        };

    assertThrows(TestException.class, initializeGlide);
    // Make sure the second exception isn't hidden by some Glide initialization related exception.
    assertThrows(TestException.class, initializeGlide);
  }
}

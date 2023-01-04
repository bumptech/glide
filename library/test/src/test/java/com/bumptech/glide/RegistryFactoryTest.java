package com.bumptech.glide;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.tests.TearDownGlide;
import com.bumptech.glide.util.GlideSuppliers.GlideSupplier;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RegistryFactoryTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final Context context = ApplicationProvider.getApplicationContext();

  private static final class TestException extends RuntimeException {
    private static final long serialVersionUID = 2334956185897161236L;
  }

  @Test
  public void create_whenCalledTwiceWithThrowingModule_throwsOriginalException() {
    AppGlideModule throwingAppGlideModule =
        new AppGlideModule() {
          @Override
          public void registerComponents(
              @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
            throw new TestException();
          }
        };

    Glide glide = Glide.get(context);
    GlideSupplier<Registry> registrySupplier =
        RegistryFactory.lazilyCreateAndInitializeRegistry(
            glide, /* manifestModules= */ ImmutableList.of(), throwingAppGlideModule);

    assertThrows(
        TestException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            registrySupplier.get();
          }
        });

    assertThrows(
        TestException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            registrySupplier.get();
          }
        });
  }
}

package com.bumptech.glide.load.engine;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

public class DecodeJobClassLoadingTest {
  @Test
  public void decodeJob_canResolveFieldsWithoutJavaUtilFunctionSupplier() throws Exception {
    String className = DecodeJob.class.getName();
    // Host JVMs provide Supplier even when Robolectric targets Android 23. Reject it explicitly
    // to catch references to this API-24 class in DecodeJob's runtime field types.
    ClassLoader loader =
        new ClassLoader(getClass().getClassLoader()) {
          @Override
          protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals("java.util.function.Supplier")) {
              throw new ClassNotFoundException(name);
            }
            if (name.startsWith("com.bumptech.glide.load.engine.")) {
              Class<?> result = findLoadedClass(name);
              if (result == null) {
                try (InputStream input =
                    getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                  if (input == null) {
                    throw new ClassNotFoundException(name);
                  }
                  byte[] bytecode = ByteStreams.toByteArray(input);
                  result = defineClass(name, bytecode, 0, bytecode.length);
                } catch (IOException e) {
                  throw new ClassNotFoundException(name, e);
                }
              }
              if (resolve) {
                resolveClass(result);
              }
              return result;
            }
            return super.loadClass(name, resolve);
          }
        };

    assertThat(loader.loadClass(className).getDeclaredFields()).isNotEmpty();
  }
}

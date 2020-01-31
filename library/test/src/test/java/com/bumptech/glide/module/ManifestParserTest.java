package com.bumptech.glide.module;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
@SuppressWarnings("deprecation")
public class ManifestParserTest {
  private static final String MODULE_VALUE = "GlideModule";

  @Mock private Context context;
  private ManifestParser parser;
  private ApplicationInfo applicationInfo;

  @Before
  public void setUp() throws PackageManager.NameNotFoundException {
    MockitoAnnotations.initMocks(this);
    applicationInfo = new ApplicationInfo();
    applicationInfo.metaData = new Bundle();

    String packageName = "com.bumptech.test";
    when(context.getPackageName()).thenReturn(packageName);

    PackageManager pm = mock(PackageManager.class);
    when(pm.getApplicationInfo(eq(packageName), eq(PackageManager.GET_META_DATA)))
        .thenReturn(applicationInfo);
    when(context.getPackageManager()).thenReturn(pm);

    parser = new ManifestParser(context);
  }

  @Test
  public void testParse_returnsEmptyListIfNoModulesListed() {
    assertThat(parser.parse()).isEmpty();
  }

  @Test
  public void testParse_withSingleValidModuleName_returnsListContainingModule() {
    addModuleToManifest(TestModule1.class);

    List<GlideModule> modules = parser.parse();
    assertThat(modules).hasSize(1);
    assertThat(modules.get(0)).isInstanceOf(TestModule1.class);
  }

  @Test
  public void testParse_withMultipleValidModuleNames_returnsListContainingModules() {
    addModuleToManifest(TestModule1.class);
    addModuleToManifest(TestModule2.class);

    List<GlideModule> modules = parser.parse();
    assertThat(modules).hasSize(2);

    assertThat(modules).contains(new TestModule1());
    assertThat(modules).contains(new TestModule2());
  }

  @Test
  public void testParse_withValidModuleName_ignoresMetadataWithoutGlideModuleValue() {
    applicationInfo.metaData.putString(TestModule1.class.getName(), MODULE_VALUE + "test");

    assertThat(parser.parse()).isEmpty();
  }

  @Test(expected = RuntimeException.class)
  public void testThrows_whenModuleNameNotFound() {
    addToManifest("fakeClassName");

    parser.parse();
  }

  @Test(expected = RuntimeException.class)
  public void testThrows_whenClassInManifestIsNotAModule() {
    addModuleToManifest(InvalidClass.class);

    parser.parse();
  }

  @Test(expected = RuntimeException.class)
  public void testThrows_whenPackageNameNotFound() {
    when(context.getPackageName()).thenReturn("fakePackageName");

    parser.parse();
  }

  private void addModuleToManifest(Class<?> moduleClass) {
    addToManifest(moduleClass.getName());
  }

  private void addToManifest(String key) {
    applicationInfo.metaData.putString(key, MODULE_VALUE);
  }

  private static class InvalidClass {}

  public static class TestModule1 implements GlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {}

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {}

    @Override
    public boolean equals(Object o) {
      return o instanceof TestModule1;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

  public static class TestModule2 implements GlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {}

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {}

    @Override
    public boolean equals(Object o) {
      return o instanceof TestModule2;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }
}

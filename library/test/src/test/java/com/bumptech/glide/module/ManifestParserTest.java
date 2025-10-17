package com.bumptech.glide.module;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
@Config(sdk = ROBOLECTRIC_SDK)
@SuppressWarnings("deprecation")
public class ManifestParserTest {
  private static final String MODULE_VALUE = "GlideModule";
  private static final String PACKAGE_NAME = "com.bumptech.test";

  @Mock private Context context;
  private ManifestParser parser;
  private ApplicationInfo applicationInfo;

  @Before
  public void setUp() throws PackageManager.NameNotFoundException {
    MockitoAnnotations.initMocks(this);
    applicationInfo = new ApplicationInfo();
    applicationInfo.metaData = new Bundle();

    when(context.getPackageName()).thenReturn(PACKAGE_NAME);

    PackageManager pm = mock(PackageManager.class);
    when(pm.getApplicationInfo(eq(PACKAGE_NAME), eq(PackageManager.GET_META_DATA)))
        .thenReturn(applicationInfo);
    when(context.getPackageManager()).thenReturn(pm);

    parser = new ManifestParser(context);
  }

  // TODO(#4977): Remove this after the bug in Compose's previews is fixed.
  @Test
  public void parse_withNullApplicationInfo_doesNotThrow() throws NameNotFoundException {
    PackageManager pm = mock(PackageManager.class);
    when(pm.getApplicationInfo(anyString(), anyInt())).thenReturn(null);
    when(context.getPackageManager()).thenReturn(pm);

    parser = new ManifestParser(context);
    parser.parse();
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

  @Test
  public void parse_withNullMetadata_doesNotThrow() throws NameNotFoundException {
    PackageManager pm = mock(PackageManager.class);
    ApplicationInfo applicationInfo = new ApplicationInfo();
    applicationInfo.metaData = null;
    when(pm.getApplicationInfo(eq(PACKAGE_NAME), eq(PackageManager.GET_META_DATA)))
        .thenReturn(applicationInfo);
    when(context.getPackageManager()).thenReturn(pm);

    parser.parse();
  }

  @Test
  public void parse_withMissingName_doesNotThrow() throws NameNotFoundException {
    PackageManager pm = mock(PackageManager.class);
    doThrow(new NameNotFoundException("name")).when(pm).getApplicationInfo(anyString(), anyInt());
    when(context.getPackageManager()).thenReturn(pm);

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

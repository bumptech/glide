package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RegistryTest {

  @Mock private ModelLoaderFactory<Model, Data> modelLoaderFactory;
  @Mock private ResourceDecoder<Data, ResourceOne> resourceOneDecoder;
  @Mock private ResourceDecoder<Data, ResourceTwo> resourceTwoDecoder;
  @Mock private ResourceTranscoder<ResourceOne, TranscodeOne> resourceOneTranscodeOneTranscoder;
  private Registry registry;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    registry = new Registry();
  }

  @Test
  public void getRegisteredResourceClasses_withNoResources_isEmpty() {
    assertThat(getRegisteredResourceClasses()).isEmpty();
  }

  @Test
  public void getRegisteredResourceClasses_withOneDataClass_noResourceClasses_isEmpty() {
    registry.append(Model.class, Data.class, modelLoaderFactory);
    assertThat(getRegisteredResourceClasses()).isEmpty();
  }

  @Test
  public void getRegisteredResourceClasses_withOneDataAndResourceClass_noTranscodeClass_isEmpty() {
    registry.append(Model.class, Data.class, modelLoaderFactory);
    registry.append(Data.class, ResourceOne.class, resourceOneDecoder);
    assertThat(getRegisteredResourceClasses()).isEmpty();
  }

  @Test
  public void getRegisteredResourceClasses_withOneDataAndResourceAndTranscodeClass_isNotEmpty() {
    registry.append(Model.class, Data.class, modelLoaderFactory);
    registry.append(Data.class, ResourceOne.class, resourceOneDecoder);
    registry.register(ResourceOne.class, TranscodeOne.class, resourceOneTranscodeOneTranscoder);
    assertThat(getRegisteredResourceClasses()).containsExactly(ResourceOne.class);
  }

  @Test
  public void getRegisteredResourceClasses_withMissingTranscodeForOneOfTwoResources_isNotEmpty() {
    // The loop allows us to make sure that the order in which we call getRegisteredResourceClasses
    // doesn't affect the output.
    for (int i = 0; i < 2; i++) {
      Registry registry = new Registry();
      registry.append(Model.class, Data.class, modelLoaderFactory);

      registry.append(Data.class, ResourceOne.class, resourceOneDecoder);
      registry.append(Data.class, ResourceTwo.class, resourceTwoDecoder);

      registry.register(ResourceOne.class, TranscodeOne.class, resourceOneTranscodeOneTranscoder);

      List<Class<?>> resourceOneClasses;
      List<Class<?>> resourceTwoClasses;
      if (i == 0) {
        resourceOneClasses =
            registry.getRegisteredResourceClasses(
                Model.class, ResourceOne.class, TranscodeOne.class);
        resourceTwoClasses =
            registry.getRegisteredResourceClasses(
                Model.class, ResourceTwo.class, TranscodeOne.class);
      } else {
        resourceTwoClasses =
            registry.getRegisteredResourceClasses(
                Model.class, ResourceTwo.class, TranscodeOne.class);
        resourceOneClasses =
            registry.getRegisteredResourceClasses(
                Model.class, ResourceOne.class, TranscodeOne.class);
      }
      // ResourceOne has a corresponding transcode class, so we should return it.
      assertThat(resourceOneClasses).containsExactly(ResourceOne.class);
      // ResourceTwo has no matching transcode class, so we shouldn't return it.
      assertThat(resourceTwoClasses).isEmpty();
    }
  }

  @Test
  public void getRegisteredResourceClasses_withOneOfTwoMissingTranscoders_isNotEmpty() {
    // The loop allows us to make sure that the order in which we call getRegisteredResourceClasses
    // doesn't affect the output.
    for (int i = 0; i < 2; i++) {
      Registry registry = new Registry();
      registry.append(Model.class, Data.class, modelLoaderFactory);

      registry.append(Data.class, ResourceOne.class, resourceOneDecoder);

      registry.register(ResourceOne.class, TranscodeOne.class, resourceOneTranscodeOneTranscoder);

      List<Class<?>> transcodeOneClasses;
      List<Class<?>> transcodeTwoClasses;
      if (i == 0) {
        transcodeOneClasses =
            registry.getRegisteredResourceClasses(
                Model.class, ResourceOne.class, TranscodeOne.class);
        transcodeTwoClasses =
            registry.getRegisteredResourceClasses(
                Model.class, ResourceOne.class, TranscodeTwo.class);
      } else {
        transcodeTwoClasses =
            registry.getRegisteredResourceClasses(
                Model.class, ResourceOne.class, TranscodeTwo.class);
        transcodeOneClasses =
            registry.getRegisteredResourceClasses(
                Model.class, ResourceOne.class, TranscodeOne.class);
      }
      // TranscodeOne has a corresponding ResourceTranscoder, so we expect to see the resource
      // class.
      assertThat(transcodeOneClasses).containsExactly(ResourceOne.class);
      // TranscodeTwo has no corresponding ResourceTranscoder class, so we shouldn't return the
      // resource class.
      assertThat(transcodeTwoClasses).isEmpty();
    }
  }

  private List<Class<?>> getRegisteredResourceClasses() {
    return registry.getRegisteredResourceClasses(
        Model.class, ResourceOne.class, TranscodeOne.class);
  }

  private static final class Model {
    // Empty class to represent model classes for readability.
  }

  private static final class Data {
    // Empty class to represent data classes for readability.
  }

  private static final class ResourceOne {
    // Empty class to represent resource classes for readability.
  }

  private static final class ResourceTwo {
    // Empty class to represent another resource class for readability.
  }

  private static final class TranscodeOne {
    // Empty class to represent transcode classes for readability.
  }

  private static final class TranscodeTwo {
    // Empty class to represent transcode classes for readability.
  }
}

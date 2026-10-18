package com.bumptech.glide.load.engine;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.os.Looper;
import com.google.common.truth.Correspondence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class ResourceRecyclerTest {

  private ResourceRecycler recycler;

  @Before
  public void setUp() {
    recycler = new ResourceRecycler();
  }

  @Test
  public void recycle_withoutForceNextFrame_recyclesResourceSynchronously() {
    Resource<?> resource = mockResource();
    Shadows.shadowOf(Looper.getMainLooper()).pause();
    recycler.recycle(resource, /* forceNextFrame= */ false);
    verify(resource).recycle();
  }

  @Test
  public void recycle_withForceNextFrame_postsRecycle() {
    Resource<?> resource = mockResource();
    Shadows.shadowOf(Looper.getMainLooper()).pause();
    recycler.recycle(resource, /* forceNextFrame= */ true);
    verify(resource, never()).recycle();
    Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks();
    verify(resource).recycle();
  }

  @Test
  public void testDoesNotRecycleChildResourceSynchronously() {
    Resource<?> parent = mockResource();
    final Resource<?> child = mockResource();
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                recycler.recycle(child, /* forceNextFrame= */ false);
                return null;
              }
            })
        .when(parent)
        .recycle();

    Shadows.shadowOf(Looper.getMainLooper()).pause();

    recycler.recycle(parent, /* forceNextFrame= */ false);

    verify(parent).recycle();
    verify(child, never()).recycle();

    Shadows.shadowOf(Looper.getMainLooper()).runOneTask();

    verify(child).recycle();
  }

  @Test
  public void recycle_withChild_asyncTraceable() {
    final Resource<?> child = mockResource();
    Throwable someCause = new Throwable("Some simulated cause.");
    IllegalStateException testEx = new IllegalStateException("Simulated error for test.", someCause);
    doThrow(testEx).when(child).recycle();
    Resource<?> parent = mockResource();
    class ChildRecycler implements Answer<Void> {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        recycler.recycle(child, /* forceNextFrame= */ false);
        return null;
      }
    }
    doAnswer(new ChildRecycler()).when(parent).recycle();

    Shadows.shadowOf(Looper.getMainLooper()).pause();
    recycler.recycle(parent, /* forceNextFrame= */ false);
    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> Shadows.shadowOf(Looper.getMainLooper()).runOneTask()
    );
    assertSame("Original exception is thrown", testEx, ex);
    assertSame("Cause is kept", someCause, ex.getCause());
    assertThat(fullStackOf(ex))
        .comparingElementsUsing(className())
        .contains(ChildRecycler.class.getName());
  }

  private static Correspondence<StackTraceElement, String> className() {
    return Correspondence.from(
        (actual, expected) -> actual.getClassName().equals(expected),
        "StackTraceElement.className"
    );
  }

  private static List<StackTraceElement> fullStackOf(Throwable t) {
    List<StackTraceElement> stack = new ArrayList<>();
    do {
      stack.addAll(Arrays.asList(t.getStackTrace()));
      t = t.getCause();
    } while (t != null);
    return stack;
  }
}

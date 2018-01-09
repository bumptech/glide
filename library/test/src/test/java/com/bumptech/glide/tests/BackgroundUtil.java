package com.bumptech.glide.tests;

public final class BackgroundUtil {

  private BackgroundUtil() {
    // Utility class.
  }

  public static void testInBackground(BackgroundTester test) throws InterruptedException {
    TestThread thread = new TestThread(test);
    thread.start();
    thread.join();
    if (thread.exception != null) {
      throw new RuntimeException(thread.exception);
    }
  }

  private static final class TestThread extends Thread {
    private final BackgroundTester test;
    private Exception exception;

    private TestThread(BackgroundTester test) {
      this.test = test;
    }

    @Override
    public void run() {
      super.run();
      try {
        test.runTest();
      } catch (Exception e) {
        exception = e;
      }
    }
  }

  public interface BackgroundTester {
    void runTest();
  }
}

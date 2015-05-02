package com.bumptech.glide.tests;

public class BackgroundUtil {
  public interface BackgroundTester {
    public void runTest() throws Exception;
  }

  public static void testInBackground(BackgroundTester test) throws InterruptedException {
    TestThread thread = new TestThread(test);
    thread.start();
    thread.join();
    if (thread.exception != null) {
      throw new RuntimeException(thread.exception);
    }
  }

  private static class TestThread extends Thread {
    private Exception exception;
    private BackgroundTester test;

    public TestThread(BackgroundTester test) {
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
}

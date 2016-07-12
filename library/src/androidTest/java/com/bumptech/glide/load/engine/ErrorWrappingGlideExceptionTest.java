package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ErrorWrappingGlideExceptionTest {

    @Test(expected = NullPointerException.class)
    public void testNullErrorCauseThrowsException() throws Exception {
        throw new ErrorWrappingGlideException(null);
    }

    @Test
    public void testGetCauseReturnsSameError() throws Exception {
        Error cause = new OutOfMemoryError("Test");
        ErrorWrappingGlideException exception = new ErrorWrappingGlideException(cause);
        assertSame("Cause should be our error", cause, exception.getCause());
    }
}
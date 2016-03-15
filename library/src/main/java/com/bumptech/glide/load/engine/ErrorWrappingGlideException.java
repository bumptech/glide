package com.bumptech.glide.load.engine;

/**
 * An exception class used for wrapping and distinguishing errors such as {@link OutOfMemoryError}.
 */
public class ErrorWrappingGlideException extends Exception {
    public ErrorWrappingGlideException(Error error) {
        super(error);
        if (error == null) {
            throw new NullPointerException("The causing error must not be null");
        }
    }

    @Override
    public Error getCause() {
        // cast is safe because constructor ensures there must be a cause, and it must be an Error
        return (Error) super.getCause();
    }
}
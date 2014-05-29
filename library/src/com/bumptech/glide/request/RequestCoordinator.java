package com.bumptech.glide.request;

import com.bumptech.glide.request.target.Target;

/**
 * An interface for coordinating multiple requests with the same {@link Target}.
 */
public interface RequestCoordinator {

    /**
     * Returns true if the {@link Request} can display a loaded bitmap.
     *
     * @param request The {@link Request} requesting permission to display a bitmap.
     */
    public boolean canSetImage(Request request);

    /**
     * Returns true if the {@link Request} can display a placeholder.
     *
     * @param request The {@link Request} requesting permission to display a placeholder.
     */
    public boolean canSetPlaceholder(Request request);

    /**
     * Returns true if any coordinated {@link Request} has successfully completed.
     *
     * @see Request#isComplete()
     */
    public boolean isAnyRequestComplete();
}

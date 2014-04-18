package com.bumptech.glide.presenter;

/**
 * An interface used to coordinate two {@link ImagePresenter} objects acting on the same target.
 */
public interface ImagePresenterCoordinator {

    /**
     * Determines if a presenter can display a loaded bitmap
     *
     * @param presenter The presenter requesting permission to display a bitmap
     * @return True iff the presenter can display a bitmap
     */
    public boolean canSetImage(ImagePresenter presenter);

    /**
     * Determines if a presenter can display a placeholder
     *
     * @param presenter The presenter requesting permission to display a placeholder
     * @return True iff the presenter can display a placeholder
     */
    public boolean canSetPlaceholder(ImagePresenter presenter);

    /**
     * Determines if a presenter can call it's callback when an image load completes.
     *
     * @param presenter The presenter requesting permission to call it's image ready callback.
     * @return True iff the presenter can call it's callback.
     */
    public boolean canCallReadyCallback(ImagePresenter presenter);

    /**
     * Determines if a presenter can call it's callback when an image load fails.
     *
     * @param presenter The presenter requesting permission to call it's load failed callback.
     * @return True iff the presenter can call it's callback.
     */
    public boolean canCallErrorCallback(ImagePresenter presenter);
}

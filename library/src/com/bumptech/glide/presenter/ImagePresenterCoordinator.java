package com.bumptech.glide.presenter;

/**
 * An interface used to coordinate two {@link ImagePresenter} objects acting on the same target.
 */
public interface ImagePresenterCoordinator {

    /**
     * Returns true if the presenter can display a loaded bitmap
     *
     * @param presenter The presenter requesting permission to display a bitmap
     */
    public boolean canSetImage(ImagePresenter presenter);

    /**
     * Returns true if the presenter can display a placeholder
     *
     * @param presenter The presenter requesting permission to display a placeholder
     */
    public boolean canSetPlaceholder(ImagePresenter presenter);

    /**
     * Returns true if any image other than a placeholder has been set by any of the presenters coordinated by this
     * object.
     */
    public boolean isAnyImageSet();
}

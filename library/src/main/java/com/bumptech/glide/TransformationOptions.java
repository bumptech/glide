package com.bumptech.glide;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.util.Preconditions;

public abstract class TransformationOptions<CHILD extends TransformationOptions<CHILD, ResourceType>, ResourceType>
        implements Cloneable {
    private Transformation<ResourceType> transformation = UnitTransformation.get();
    private boolean isTransformationSet;

    /**
     * Transform resources with the given {@link Transformation}s. Replaces any existing transformation or
     * transformations.
     *
     * @param transformation the transformation to apply.
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public final CHILD transform(Transformation<ResourceType> transformation) {
        this.transformation = Preconditions.checkNotNull(transformation);
        isTransformationSet = true;

        return (CHILD) this;
    }

    /**
     * Removes the current {@link com.bumptech.glide.load.Transformation}.
     *
     * @return This request builder.
     */
    public final CHILD dontTransform() {
        return transform(UnitTransformation.<ResourceType>get());
    }

    protected void applyFitCenter() {
        // Do nothing by default.
    }

    protected void applyCenterCrop() {
        // Do nothing by default.
    }

    @SuppressWarnings("unchecked")
    public final CHILD apply(TransformationOptions<?, ResourceType> other) {
        if (other.isTransformationSet) {
            transformation = other.transformation;
        }
        return self();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final CHILD clone() {
        try {
            return (CHILD) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    final Transformation<ResourceType> getTransformation() {
        return transformation;
    }

    final boolean isTransformationSet() {
        return isTransformationSet;
    }

    @SuppressWarnings("unchecked")
    private final CHILD self() {
        return (CHILD) this;
    }
}

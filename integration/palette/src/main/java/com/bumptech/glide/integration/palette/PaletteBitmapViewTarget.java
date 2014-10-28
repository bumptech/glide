package com.bumptech.glide.integration.palette;

import android.widget.ImageView;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.ViewTarget;

/**
 * Created by judds on 10/27/14.
 */
public class PaletteBitmapViewTarget extends ImageViewTarget<PaletteBitmap> {

    public PaletteBitmapViewTarget(ImageView view) {
        super(view);
    }

    @Override
    protected void setResource(PaletteBitmap resource) {
        view.setImageBitmap(resource.bitmap);
    }
}

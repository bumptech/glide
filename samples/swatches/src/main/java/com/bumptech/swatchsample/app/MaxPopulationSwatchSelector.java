package com.bumptech.swatchsample.app;

import android.support.v7.graphics.Palette;

import com.bumptech.glide.integration.palette.PaletteBitmapViewTarget;

class MaxPopulationSwatchSelector implements PaletteBitmapViewTarget.Builder.SwatchSelector {
    @Override
    public Palette.Swatch select(Palette palette) {
        Palette.Swatch maxSwatch = null;
        for (Palette.Swatch swatch : palette.getSwatches()) {
            if (maxSwatch == null || maxSwatch.getPopulation() < swatch.getPopulation()) {
                maxSwatch = swatch;
            }
        }

        return maxSwatch;
    }
}

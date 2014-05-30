package com.bumptech.glide;

public enum MemoryCategory {
    LOW(0.5f),
    NORMAL(1f),
    HIGH(1.5f);

    private float multiplier;

    MemoryCategory(float multiplier) {
        this.multiplier = multiplier;
    }

    public float getMultiplier() {
        return multiplier;
    }
}

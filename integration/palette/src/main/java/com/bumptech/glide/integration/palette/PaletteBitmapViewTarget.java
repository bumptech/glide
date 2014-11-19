package com.bumptech.glide.integration.palette;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.graphics.Palette;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.ImageViewTarget;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link com.bumptech.glide.request.target.Target} that can display an {@link android.graphics.Bitmap} in an
 * {@link android.widget.ImageView} and additionally set some other view's colors based on colors retrieved
 * from a {@link android.support.v7.graphics.Palette}.
 */
public class PaletteBitmapViewTarget extends ImageViewTarget<PaletteBitmap> {

    public PaletteBitmapViewTarget(ImageView view) {
        super(view);
    }

    @Override
    protected void setResource(PaletteBitmap resource) {
        view.setImageBitmap(resource.bitmap);
    }

    /**
     * A builder to easily handle the different swatches and set the background and text colors.
     */
    public static final class Builder {
        /**
         * Retrieve a swatch from a Palette based on some criteria which the implementors define.
         * It is not required to retrieve any swatch if no perfect match found.
         */
        public interface SwatchSelector {
            Palette.Swatch select(Palette palette);
        }

        /**
         * A target that can set its properties based on a {@link android.support.v7.graphics.Palette.Swatch}
         * such as background, text color, usually something visual and colorful. Must handle lack of a swatch.
         */
        public interface SwatchTarget {
            void set(Palette.Swatch swatch);
        }

        public static final SwatchSelector VIBRANT = new SwatchSelector() {
            public Palette.Swatch select(Palette palette) {
                return palette.getVibrantSwatch();
            }
        };
        public static final SwatchSelector VIBRANT_LIGHT = new SwatchSelector() {
            public Palette.Swatch select(Palette palette) {
                return palette.getLightVibrantSwatch();
            }
        };
        public static final SwatchSelector VIBRANT_DARK = new SwatchSelector() {
            public Palette.Swatch select(Palette palette) {
                return palette.getDarkVibrantSwatch();
            }
        };

        public static final SwatchSelector MUTED = new SwatchSelector() {
            public Palette.Swatch select(Palette palette) {
                return palette.getMutedSwatch();
            }
        };
        public static final SwatchSelector MUTED_LIGHT = new SwatchSelector() {
            public Palette.Swatch select(Palette palette) {
                return palette.getLightMutedSwatch();
            }
        };
        public static final SwatchSelector MUTED_DARK = new SwatchSelector() {
            public Palette.Swatch select(Palette palette) {
                return palette.getDarkMutedSwatch();
            }
        };

        private final ImageView image;
        private int defaultFallbackBackground;
        private int defaultFallbackTextColor;

        private final List<ActionGroup> actions = new LinkedList<ActionGroup>();
        private ActionGroup current;

        public Builder(ImageView image) {
            this.image = image;
            this.defaultFallbackBackground = Color.TRANSPARENT;
            this.defaultFallbackTextColor = getDefaultTextColor(image.getContext());
        }


        public Builder defaultFallback(int fallbackBackground, int fallbackTextColor) {
            this.defaultFallbackBackground = fallbackBackground;
            this.defaultFallbackTextColor = fallbackTextColor;
            return this;
        }

        public int getDefaultFallbackBackground() {
            return defaultFallbackBackground;
        }

        public int getDefaultFallbackTextColor() {
            return defaultFallbackTextColor;
        }

        public ImageView getImage() {
            return image;
        }

        private void checkCurrent() {
            if (current == null) {
                throw new IllegalStateException("You must call .swatch() first.");
            }
        }

        public Builder swatch(SwatchSelector selector, int fallbackBackground, int fallbackTextColor) {
            current = new ActionGroup(selector);
            current.fallbackBackground = fallbackBackground;
            current.fallbackTextColor = fallbackTextColor;
            actions.add(current);
            return this;
        }

        public Builder swatch(SwatchSelector selector) {
            return swatch(selector, defaultFallbackBackground, defaultFallbackTextColor);
        }

        public Builder title(TextView text) {
            background(text);
            titleText(text);
            return this;
        }

        public Builder titleText(TextView titleText) {
            checkCurrent();
            current.titleTexts.add(titleText);
            return this;
        }


        public Builder body(TextView text) {
            background(text);
            bodyText(text);
            return this;
        }

        public Builder bodyText(TextView bodyText) {
            checkCurrent();
            current.bodyTexts.add(bodyText);
            return this;
        }


        public Builder background(View view) {
            return background(view, 0xFF);
        }

        public Builder background(View view, int alpha) {
            checkCurrent();
            current.backgrounds.add(new ActionGroup.ViewHolder(view, alpha));
            return this;
        }

        public Builder custom(SwatchTarget any) {
            checkCurrent();
            current.customs.add(any);
            return this;
        }

        public PaletteBitmapViewTarget build() {
            current = null;
            return new PaletteBitmapViewTarget(image) {
                // make a copy in case the user keeps calling Builder methods after build()
                private final List<ActionGroup> actions = new ArrayList<ActionGroup>(Builder.this.actions);

                @Override
                protected void setResource(PaletteBitmap resource) {
                    super.setResource(resource);
                    for (ActionGroup action : actions) {
                        action.execute(resource.palette);
                    }
                }
            };
        }

        private static int getDefaultTextColor(Context context) {
            Resources.Theme theme = context.getTheme();
            TypedValue outValue = new TypedValue();
            theme.resolveAttribute(android.R.attr.textViewStyle, outValue, true);
            int[] attributes = {android.R.attr.textColor};
            TypedArray styledAttributes = theme.obtainStyledAttributes(outValue.resourceId, attributes);
            int color = styledAttributes.getColor(0, Color.BLACK);
            styledAttributes.recycle();
            return color;
        }

        private static class ActionGroup {
            private final SwatchSelector selector;

            private final List<TextView> titleTexts = new LinkedList<TextView>();
            private final List<TextView> bodyTexts = new LinkedList<TextView>();
            private final List<ViewHolder> backgrounds = new LinkedList<ViewHolder>();
            private final List<SwatchTarget> customs = new LinkedList<SwatchTarget>();
            private int fallbackBackground;
            private int fallbackTextColor;

            public ActionGroup(SwatchSelector selector) {
                this.selector = selector;
            }

            public void execute(Palette palette) {
                Palette.Swatch swatch = selector.select(palette);

                int titleTextColor = swatch != null ? swatch.getTitleTextColor() : fallbackTextColor;
                for (TextView titleText : titleTexts) {
                    titleText.setTextColor(titleTextColor);
                }

                int bodyTextColor = swatch != null ? swatch.getBodyTextColor() : fallbackTextColor;
                for (TextView bodyText : bodyTexts) {
                    bodyText.setTextColor(bodyTextColor);
                }

                int backgroundColor = swatch != null ? swatch.getRgb() : fallbackBackground;
                for (ViewHolder background : backgrounds) {
                    background.view.setBackgroundColor(background.applyAlpha(backgroundColor));
                }

                for (SwatchTarget custom : customs) {
                    custom.set(swatch);
                }
            }

            private static class ViewHolder {
                private View view;
                private int alpha;

                public ViewHolder(View view, int alpha) {
                    this.view = view;
                    this.alpha = alpha;
                }

                public int applyAlpha(int color) {
                    if (Color.alpha(color) < 0xFF) {
                        // already has alpha set, don't modify it
                        return color;
                    }
                    return color & 0x00FFFFFF | alpha << 24;
                }
            }
        }
    }
}

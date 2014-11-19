package com.bumptech.swatchsample.app;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.integration.palette.PaletteBitmap;
import com.bumptech.glide.integration.palette.PaletteBitmapViewTarget;
import com.bumptech.glide.integration.palette.PaletteBitmapViewTarget.Builder;

/**
 * Display an image and some text with colors calculated with {@link android.support.v7.graphics.Palette}.
 */
class PaletteAdapter extends BaseAdapter {
    private final Uri[] urls;
    private final GenericRequestBuilder<Uri, ?, ?, PaletteBitmap> glide;

    public PaletteAdapter(Uri[] urls, GenericRequestBuilder<Uri, ?, ?, PaletteBitmap> glide) {
        this.urls = urls;
        this.glide = glide;
    }

    @Override
    public int getCount() {
        return urls.length;
    }

    @Override
    public Uri getItem(int position) {
        return urls[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        bindView(position, holder);
        return convertView;
    }

    private void bindView(int position, final ViewHolder holder) {
        Uri url = getItem(position);
        holder.titleText.setText(url.getPath());
        holder.bodyText.setText(url.toString());
        glide.load(url).into(holder.target);
    }

    private static class ViewHolder {
        private static final Builder.SwatchSelector MAX_POPULATION = new MaxPopulationSwatchSelector();

        final View view;

        final ImageView image;
        final TextView titleText;
        final TextView bodyText;
        final TextView count;

        final PaletteBitmapViewTarget target;

        ViewHolder(View view) {
            this.view = view;
            image = (ImageView) view.findViewById(R.id.image);
            titleText = (TextView) view.findViewById(R.id.titleText);
            bodyText = (TextView) view.findViewById(R.id.bodyText);
            count = (TextView) view.findViewById(R.id.count);

            target = new PaletteBitmapViewTarget.Builder(image)
                    .swatch(Builder.MUTED_DARK, Color.DKGRAY, Color.WHITE)
                    .background(view)
                    .bodyText(bodyText)
                    .swatch(Builder.VIBRANT)
                    .background(titleText, 0x60)
                    .titleText(titleText)
                    .swatch(MAX_POPULATION)
                    .custom(new Builder.SwatchTarget() {
                        @Override
                        @SuppressWarnings("deprecation")
                        public void set(Palette.Swatch swatch) {
                            ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
                            drawable.setIntrinsicWidth(10);
                            drawable.setIntrinsicHeight(10);
                            drawable.setColorFilter(swatch.getRgb(), PorterDuff.Mode.SRC_OVER);
                            count.setBackgroundDrawable(drawable);
                            count.setTextColor(swatch.getBodyTextColor());

                            count.setText(Integer.toString(swatch.getPopulation()));
                        }
                    })
                    .build();
        }
    }
}

package com.bumptech.glide.samples.gallery;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.bumptech.glide.util.Preconditions;
import java.util.Collections;
import java.util.List;

/** Displays {@link com.bumptech.glide.samples.gallery.MediaStoreData} in a recycler view. */
class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ListViewHolder>
    implements ListPreloader.PreloadSizeProvider<MediaStoreData>,
        ListPreloader.PreloadModelProvider<MediaStoreData> {

  private final List<MediaStoreData> data;
  private final int screenWidth;
  private final GlideRequest<Drawable> requestBuilder;

  private int[] actualDimensions;

  RecyclerAdapter(Context context, List<MediaStoreData> data, GlideRequests glideRequests) {
    this.data = data;
    requestBuilder = glideRequests.asDrawable().fitCenter();

    setHasStableIds(true);

    screenWidth = getScreenWidth(context);
  }

  @NonNull
  @Override
  public ListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
    final View view = inflater.inflate(R.layout.recycler_item, viewGroup, false);
    view.getLayoutParams().width = screenWidth;

    if (actualDimensions == null) {
      view.getViewTreeObserver()
          .addOnPreDrawListener(
              new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                  if (actualDimensions == null) {
                    actualDimensions = new int[] {view.getWidth(), view.getHeight()};
                  }
                  view.getViewTreeObserver().removeOnPreDrawListener(this);
                  return true;
                }
              });
    }

    return new ListViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ListViewHolder viewHolder, int position) {
    MediaStoreData current = data.get(position);

    Key signature =
        new MediaStoreSignature(current.mimeType, current.dateModified, current.orientation);

    requestBuilder.clone().signature(signature).load(current.uri).into(viewHolder.image);
  }

  @Override
  public long getItemId(int position) {
    return data.get(position).rowId;
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  @Override
  public int getItemViewType(int position) {
    return 0;
  }

  @NonNull
  @Override
  public List<MediaStoreData> getPreloadItems(int position) {
    return data.isEmpty()
        ? Collections.<MediaStoreData>emptyList()
        : Collections.singletonList(data.get(position));
  }

  @Nullable
  @Override
  public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull MediaStoreData item) {
    MediaStoreSignature signature =
        new MediaStoreSignature(item.mimeType, item.dateModified, item.orientation);
    return requestBuilder.clone().signature(signature).load(item.uri);
  }

  @Nullable
  @Override
  public int[] getPreloadSize(
      @NonNull MediaStoreData item, int adapterPosition, int perItemPosition) {
    return actualDimensions;
  }

  // Display#getSize(Point)
  @SuppressWarnings("deprecation")
  private static int getScreenWidth(Context context) {
    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = Preconditions.checkNotNull(wm).getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    return size.x;
  }

  /**
   * ViewHolder containing views to display individual {@link
   * com.bumptech.glide.samples.gallery.MediaStoreData}.
   */
  static final class ListViewHolder extends RecyclerView.ViewHolder {

    private final ImageView image;

    ListViewHolder(View itemView) {
      super(itemView);
      image = itemView.findViewById(R.id.image);
    }
  }
}

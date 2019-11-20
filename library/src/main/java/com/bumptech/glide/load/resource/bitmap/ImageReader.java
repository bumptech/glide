package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.data.InputStreamRewinder;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This is a helper class for {@link Downsampler} that abstracts out image operations from the input
 * type wrapped into a {@link DataRewinder}.
 */
interface ImageReader {
  @Nullable
  Bitmap decodeBitmap(BitmapFactory.Options options) throws IOException;

  ImageHeaderParser.ImageType getImageType() throws IOException;

  int getImageOrientation() throws IOException;

  void stopGrowingBuffers();

  final class InputStreamImageReader implements ImageReader {
    private final InputStreamRewinder dataRewinder;
    private final ArrayPool byteArrayPool;
    private final List<ImageHeaderParser> parsers;

    InputStreamImageReader(
        InputStream is, List<ImageHeaderParser> parsers, ArrayPool byteArrayPool) {
      this.byteArrayPool = Preconditions.checkNotNull(byteArrayPool);
      this.parsers = Preconditions.checkNotNull(parsers);

      dataRewinder = new InputStreamRewinder(is, byteArrayPool);
    }

    @Nullable
    @Override
    public Bitmap decodeBitmap(BitmapFactory.Options options) throws IOException {
      return BitmapFactory.decodeStream(dataRewinder.rewindAndGet(), null, options);
    }

    @Override
    public ImageHeaderParser.ImageType getImageType() throws IOException {
      return ImageHeaderParserUtils.getType(parsers, dataRewinder.rewindAndGet(), byteArrayPool);
    }

    @Override
    public int getImageOrientation() throws IOException {
      return ImageHeaderParserUtils.getOrientation(
          parsers, dataRewinder.rewindAndGet(), byteArrayPool);
    }

    @Override
    public void stopGrowingBuffers() {
      dataRewinder.fixMarkLimits();
    }
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  final class ParcelFileDescriptorImageReader implements ImageReader {
    private final ArrayPool byteArrayPool;
    private final List<ImageHeaderParser> parsers;
    private final ParcelFileDescriptorRewinder dataRewinder;

    ParcelFileDescriptorImageReader(
        ParcelFileDescriptor parcelFileDescriptor,
        List<ImageHeaderParser> parsers,
        ArrayPool byteArrayPool) {
      this.byteArrayPool = Preconditions.checkNotNull(byteArrayPool);
      this.parsers = Preconditions.checkNotNull(parsers);

      dataRewinder = new ParcelFileDescriptorRewinder(parcelFileDescriptor);
    }

    @Nullable
    @Override
    public Bitmap decodeBitmap(BitmapFactory.Options options) throws IOException {
      return BitmapFactory.decodeFileDescriptor(
          dataRewinder.rewindAndGet().getFileDescriptor(), null, options);
    }

    @Override
    public ImageHeaderParser.ImageType getImageType() throws IOException {
      return ImageHeaderParserUtils.getType(parsers, dataRewinder, byteArrayPool);
    }

    @Override
    public int getImageOrientation() throws IOException {
      return ImageHeaderParserUtils.getOrientation(parsers, dataRewinder, byteArrayPool);
    }

    @Override
    public void stopGrowingBuffers() {
      // Nothing to do here.
    }
  }
}

package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

/** {@link InputStream} specific implementation of {@link BitmapImageDecoderResourceDecoder}. */
@RequiresApi(api = 28)
public final class InputStreamBitmapImageDecoderResourceDecoder
    implements ResourceDecoder<InputStream, Bitmap> {
  private final BitmapImageDecoderResourceDecoder wrapped = new BitmapImageDecoderResourceDecoder();
  private final List<ImageHeaderParser> parsers;
  private final boolean useHeapBuffer;
  @Nullable private final ArrayPool arrayPool;
  private final boolean useArrayPool;
  private final boolean respectExifOrientation;
  private final boolean spoolInputStreamToTempFile;
  @Nullable private final File tempDir;

  public InputStreamBitmapImageDecoderResourceDecoder(
      List<ImageHeaderParser> parsers,
      boolean useHeapBuffer,
      @Nullable ArrayPool arrayPool,
      boolean useArrayPool) {
    this(
        parsers,
        useHeapBuffer,
        arrayPool,
        useArrayPool,
        /* respectExifOrientation= */ false,
        /* spoolInputStreamToTempFile= */ false,
        /* tempDir= */ null);
  }

  public InputStreamBitmapImageDecoderResourceDecoder(
      List<ImageHeaderParser> parsers,
      boolean useHeapBuffer,
      @Nullable ArrayPool arrayPool,
      boolean useArrayPool,
      boolean respectExifOrientation) {
    this(
        parsers,
        useHeapBuffer,
        arrayPool,
        useArrayPool,
        respectExifOrientation,
        /* spoolInputStreamToTempFile= */ false,
        /* tempDir= */ null);
  }

  public InputStreamBitmapImageDecoderResourceDecoder(
      List<ImageHeaderParser> parsers,
      boolean useHeapBuffer,
      @Nullable ArrayPool arrayPool,
      boolean useArrayPool,
      boolean respectExifOrientation,
      boolean spoolInputStreamToTempFile,
      @Nullable File tempDir) {
    this.parsers = parsers;
    this.useHeapBuffer = useHeapBuffer;
    this.arrayPool = arrayPool;
    this.useArrayPool = useArrayPool;
    this.respectExifOrientation = respectExifOrientation;
    this.spoolInputStreamToTempFile = spoolInputStreamToTempFile;
    this.tempDir = tempDir;
  }

  @Override
  public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
    if (!useArrayPool) {
      return true;
    }
    if (arrayPool == null) {
      return false;
    }
    ImageType type = ImageHeaderParserUtils.getType(parsers, source, arrayPool);
    return type != ImageType.UNKNOWN;
  }

  @Override
  public Resource<Bitmap> decode(
      @NonNull InputStream stream, int width, int height, @NonNull Options options)
      throws IOException {
    if (spoolInputStreamToTempFile) {
      return decodeFromTempFile(stream, width, height, options);
    }
    ByteBuffer buffer =
        useArrayPool && arrayPool != null
            ? ByteBufferUtil.fromStream(stream, useHeapBuffer, arrayPool)
            : ByteBufferUtil.fromStream(stream, useHeapBuffer);
    if (respectExifOrientation && parsers != null && !parsers.isEmpty() && arrayPool != null) {
      int orientation = ImageHeaderParserUtils.getOrientation(parsers, buffer, arrayPool);
      if (TransformationUtils.isExifOrientationRequired(orientation)) {
        Options optionsWithExif = new Options();
        optionsWithExif.putAll(options);
        optionsWithExif.set(Downsampler.IS_EXIF_ORIENTATION_REQUIRED, true);
        options = optionsWithExif;
      }
    }
    Source source = ImageDecoder.createSource(buffer);
    return wrapped.decode(source, width, height, options);
  }

  private Resource<Bitmap> decodeFromTempFile(
      @NonNull InputStream stream, int width, int height, @NonNull Options options)
      throws IOException {
    if (tempDir != null && !tempDir.exists()) {
      tempDir.mkdirs();
    }
    File tempFile =
        tempDir != null
            ? File.createTempFile("glide_stream_", ".tmp", tempDir)
            : File.createTempFile("glide_stream_", ".tmp");
    try {
      byte[] buffer =
          arrayPool != null
              ? arrayPool.get(ArrayPool.STANDARD_BUFFER_SIZE_BYTES, byte[].class)
              : new byte[ArrayPool.STANDARD_BUFFER_SIZE_BYTES];
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
        int read;
        while ((read = stream.read(buffer)) != -1) {
          out.write(buffer, 0, read);
        }
        out.flush();
      } finally {
        if (arrayPool != null) {
          arrayPool.put(buffer);
        }
      }

      if (respectExifOrientation && parsers != null && !parsers.isEmpty() && arrayPool != null) {
        try (InputStream fis = new FileInputStream(tempFile)) {
          int orientation = ImageHeaderParserUtils.getOrientation(parsers, fis, arrayPool);
          if (TransformationUtils.isExifOrientationRequired(orientation)) {
            Options optionsWithExif = new Options();
            optionsWithExif.putAll(options);
            optionsWithExif.set(Downsampler.IS_EXIF_ORIENTATION_REQUIRED, true);
            options = optionsWithExif;
          }
        }
      }

      Source source = ImageDecoder.createSource(tempFile);
      return wrapped.decode(source, width, height, options);
    } finally {
      if (!tempFile.delete()) {
        // Ignored.
      }
    }
  }
}

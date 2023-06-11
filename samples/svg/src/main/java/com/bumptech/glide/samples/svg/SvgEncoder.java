package com.bumptech.glide.samples.svg;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.caverock.androidsvg.SVG;
import java.io.File;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

/**
 * This class encodes SVG resources into bitmap form.
 *
 * Note: This class is required to make Glide's DiskCacheStrategy work correctly.
 * Glide uses this class to encode SVG resources, which allows it to cache the resulting
 * bitmaps on disk. Without this class, Glide wouldn't know how to convert SVG resources
 * into a form it can cache, which would prevent DiskCacheStrategy from functioning as expected.
 */
public class SvgEncoder implements ResourceEncoder<SVG> {
  private ArrayPool arrayPool; // An ArrayPool used in the encoding process
  private BitmapPool bitmapPool; // A BitmapPool used in the encoding process
  private BitmapEncoder bitmapEncoder; // An instance of BitmapEncoder to delegate the actual encoding process

  // Constructor that initializes the ArrayPool, BitmapPool, and BitmapEncoder
  public SvgEncoder(ArrayPool arrayPool, BitmapPool bitmapPool) {
    this.arrayPool = arrayPool;
    this.bitmapPool = bitmapPool;
    this.bitmapEncoder = new BitmapEncoder(arrayPool);
  }

  // Encodes the provided SVG resource into bitmap form
  @Override
  public boolean encode(Resource<SVG> data, File file, Options options) {
    SVG svg = data.get(); // Retrieve SVG from the resource
    Picture picture = svg.renderToPicture(); // Render SVG to Picture
    int width = picture.getWidth(); // Get width of the picture
    int height = picture.getHeight(); // Get height of the picture
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // Create Bitmap of specified size
    Canvas canvas = new Canvas(bitmap); // Create Canvas to draw the Bitmap

    // Draw the picture onto the canvas, thereby transferring it to the bitmap
    picture.draw(canvas);

    // Obtain a BitmapResource from the bitmap
    BitmapResource resource = BitmapResource.obtain(bitmap, bitmapPool);
    // Encode the BitmapResource if possible
    boolean isEncoded = resource != null && bitmapEncoder.encode(resource, file, options);

    // To free memory ASAP, recycle the bitmap
    bitmap.recycle();

    // Return the status of encoding
    return isEncoded;
  }

  // Return the EncodeStrategy of the bitmapEncoder
  @Override
  public EncodeStrategy getEncodeStrategy(Options options) {
    return bitmapEncoder.getEncodeStrategy(options);
  }
}


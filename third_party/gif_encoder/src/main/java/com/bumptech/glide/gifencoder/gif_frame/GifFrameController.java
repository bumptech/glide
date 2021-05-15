package com.bumptech.glide.gifencoder.gif_frame;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.gifencoder.AnimatedGifEncoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class GifFrameController {

  private static GifFrameController gifFrameController;

  private GifFrameController(){}

  public static GifFrameController getInstance(){
    if(gifFrameController==null) gifFrameController=new GifFrameController();
    return gifFrameController;
  }


  //// for getting all the frame Of the gif
  public ArrayList<Bitmap> getAllFrames(Context context,int sourceId){
    final ArrayList<Bitmap> bitmaps = new ArrayList<>();
    Glide.with(context)
        .asGif()
        .load(sourceId)
        .into(new SimpleTarget<GifDrawable>() {
          @Override
          public void onResourceReady(@NonNull GifDrawable resource, @Nullable Transition<? super GifDrawable> transition) {
            try {
              Object GifState = resource.getConstantState();
              Field frameLoader = GifState.getClass().getDeclaredField("frameLoader");
              frameLoader.setAccessible(true);
              Object gifFrameLoader = frameLoader.get(GifState);
              Field gifDecoder = gifFrameLoader.getClass().getDeclaredField("gifDecoder");
              gifDecoder.setAccessible(true);
              StandardGifDecoder standardGifDecoder = (StandardGifDecoder) gifDecoder.get(gifFrameLoader);
              for (int i = 0; i < standardGifDecoder.getFrameCount(); i++) {
                standardGifDecoder.advance();
                bitmaps.add(standardGifDecoder.getNextFrame());
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
    return bitmaps;
  }

  public ArrayList<Bitmap> getAllFrames(Context context, Uri uri){
    final ArrayList<Bitmap> bitmaps = new ArrayList<>();
    Glide.with(context)
        .asGif()
        .load(uri)
        .into(new SimpleTarget<GifDrawable>() {
          @Override
          public void onResourceReady(@NonNull GifDrawable resource, @Nullable Transition<? super GifDrawable> transition) {
            try {
              Object GifState = resource.getConstantState();
              Field frameLoader = GifState.getClass().getDeclaredField("frameLoader");
              frameLoader.setAccessible(true);
              Object gifFrameLoader = frameLoader.get(GifState);
              Field gifDecoder = gifFrameLoader.getClass().getDeclaredField("gifDecoder");
              gifDecoder.setAccessible(true);
              StandardGifDecoder standardGifDecoder = (StandardGifDecoder) gifDecoder.get(gifFrameLoader);
              for (int i = 0; i < standardGifDecoder.getFrameCount(); i++) {
                standardGifDecoder.advance();
                bitmaps.add(standardGifDecoder.getNextFrame());
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
    return bitmaps;
  }

  public ArrayList<Bitmap> getAllFrames(Context context, File file){
    final ArrayList<Bitmap> bitmaps = new ArrayList<>();
    Glide.with(context)
        .asGif()
        .load(file)
        .into(new SimpleTarget<GifDrawable>() {
          @Override
          public void onResourceReady(@NonNull GifDrawable resource, @Nullable Transition<? super GifDrawable> transition) {
            try {
              Object GifState = resource.getConstantState();
              Field frameLoader = GifState.getClass().getDeclaredField("frameLoader");
              frameLoader.setAccessible(true);
              Object gifFrameLoader = frameLoader.get(GifState);
              Field gifDecoder = gifFrameLoader.getClass().getDeclaredField("gifDecoder");
              gifDecoder.setAccessible(true);
              StandardGifDecoder standardGifDecoder = (StandardGifDecoder) gifDecoder.get(gifFrameLoader);
              for (int i = 0; i < standardGifDecoder.getFrameCount(); i++) {
                standardGifDecoder.advance();
                bitmaps.add(standardGifDecoder.getNextFrame());
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
    return bitmaps;
  }

  public ArrayList<Bitmap> getAllFrames(Context context,byte[] model){
    final ArrayList<Bitmap> bitmaps = new ArrayList<>();
    Glide.with(context)
        .asGif()
        .load(model)
        .into(new SimpleTarget<GifDrawable>() {
          @Override
          public void onResourceReady(@NonNull GifDrawable resource, @Nullable Transition<? super GifDrawable> transition) {
            try {
              Object GifState = resource.getConstantState();
              Field frameLoader = GifState.getClass().getDeclaredField("frameLoader");
              frameLoader.setAccessible(true);
              Object gifFrameLoader = frameLoader.get(GifState);
              Field gifDecoder = gifFrameLoader.getClass().getDeclaredField("gifDecoder");
              gifDecoder.setAccessible(true);
              StandardGifDecoder standardGifDecoder = (StandardGifDecoder) gifDecoder.get(gifFrameLoader);
              for (int i = 0; i < standardGifDecoder.getFrameCount(); i++) {
                standardGifDecoder.advance();
                bitmaps.add(standardGifDecoder.getNextFrame());
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
    return bitmaps;
  }


  /// generating gif from specified frame!

  public byte[] generateGIFFromSpecifiedFrame(Context context , int start ,int sourceId) {
    ArrayList<Bitmap> bitmaps = getAllFrames(context, sourceId);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
    encoder.start(bos);
    for(int i=start;i<bitmaps.size();i++){
      encoder.addFrame(bitmaps.get(i));
    }
    encoder.finish();
    return bos.toByteArray();
  }

  public byte[] generateGIFFromSpecifiedFrame(Context context , int start ,File source) {
    ArrayList<Bitmap> bitmaps = getAllFrames(context, source);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
    encoder.start(bos);
    for(int i=start;i<bitmaps.size();i++){
      encoder.addFrame(bitmaps.get(i));
    }
    encoder.finish();
    return bos.toByteArray();
  }

  public byte[] generateGIFFromSpecifiedFrame(Context context , int start ,byte[] source) {
    ArrayList<Bitmap> bitmaps = getAllFrames(context, source);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
    encoder.start(bos);
    for(int i=start;i<bitmaps.size();i++){
      encoder.addFrame(bitmaps.get(i));
    }
    encoder.finish();
    return bos.toByteArray();
  }

  public byte[] generateGIFFromSpecifiedFrame(Context context , int start ,Uri source) {
    ArrayList<Bitmap> bitmaps = getAllFrames(context, source);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
    encoder.start(bos);
    for(int i=start;i<bitmaps.size();i++){
      encoder.addFrame(bitmaps.get(i));
    }
    encoder.finish();
    return bos.toByteArray();
  }
}

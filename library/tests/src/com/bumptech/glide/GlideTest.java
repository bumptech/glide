package com.bumptech.glide;

import android.content.Context;
import android.net.Uri;
import android.test.ActivityTestCase;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.resize.target.ImageViewTarget;
import com.bumptech.glide.resize.target.Target;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link Glide} interface and singleton.
 */
public class GlideTest extends ActivityTestCase {
    private ImageView imageView;
    private ImageViewTarget imageViewTarget;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        imageView = new ImageView(getContext());
        //this is a quick hack to get the SizeDeterminer in ImagePresenter to think the view has been measured
        imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        imageViewTarget = new ImageViewTarget(imageView);
    }

    private Context getContext() {
        Context result = getInstrumentation().getTargetContext();
        if (result == null) {
            throw new IllegalArgumentException();
        }
        return result;
    }

    public void testFileDefaultLoader() {
        File file = new File("fake");
        Target target = Glide.with(getContext()).load(file).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    public void testUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");
        Target target = Glide.with(getContext()).loadFromImage(url).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    public void testUriDefaultLoader() {
        Uri uri = Uri.fromFile(new File("Fake"));
        Target target = Glide.with(getContext()).load(uri).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    public void testStringDefaultLoader() {
        String string = "http://www.google.com";
        Target target = Glide.with(getContext()).load(string).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    public void testIntegerDefaultLoader() {
        int integer = 1234;
        Target target = Glide.with(getContext()).load(integer).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    public void testByteArrayDefaultLoader() {
        byte[] bytes = new byte[10];
        Target target = Glide.with(getContext()).loadFromImage(bytes).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    public void testByteArrayWithIdDefaultLoader() {
        byte[] bytes = new byte[10];
        String id = "test";
        Target target = Glide.with(getContext()).loadFromImage(bytes, id).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    public void testUnregisteredModelThrowsException() {
        Float unregistered = 0.5f;
        try {
            Glide.with(getContext()).load(unregistered).into(imageViewTarget);
            fail("Expected load using model with no registered factory to throw an exception!");
        } catch (Exception e) { }
    }

    public void testUnregisteredModelWithGivenLoaderDoesNotThrow() {
        Float unregistered = 0.5f;
        StreamModelLoader<Float> mockLoader = mock(StreamModelLoader.class);
        Glide.with(getContext())
                .using(mockLoader)
                .load(unregistered)
                .into(imageViewTarget);
    }

    public void testNonDefaultModelWithRegisteredFactoryDoesNotThrow() {
        Glide glide = Glide.get(getContext());
        glide.register(Float.class, InputStream.class, new ModelLoaderFactory<Float, InputStream>() {
            @Override
            public ModelLoader<Float, InputStream> build(Context context, GenericLoaderFactory factories) {
                return mock(ModelLoader.class);
            }

            @Override
            public void teardown() {
            }
        });
        Glide.with(getContext()).load(0.5f).into(imageViewTarget);
        glide.unregister(Float.class, InputStream.class);
    }

//    public void testMemoryLeak() throws InterruptedException {
//        final int numRequests = 200;
//        final HandlerThread handlerThread = new HandlerThread("memory_leak");
//        handlerThread.start();
//        final Handler handler = new Handler(handlerThread.getLooper());
//        final AtomicInteger totalRun = new AtomicInteger();
//        Runnable doLoad = new Runnable() {
//                @Override
//                public void run() {
//                    final ImageView temp = new ImageView(getInstrumentation().getContext());
//                    temp.setBackgroundDrawable(new BitmapDrawable(
//                            Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)));
//                    temp.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
//                    Glide.using(new ModelLoader<Object>() {
//                        @Override
//                        public ResourceFetcher getResourceFetcher(Object model, int width, int height) {
//                            return new ResourceFetcher() {
//                                @Override
//                                public void loadResource(ResourceReadyCallback cb) {
//                                    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
//                                    ByteArrayOutputStream os = new ByteArrayOutputStream();
//                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
//                                    cb.onResourceReady(new ByteArrayInputStream(os.toByteArray()));
//                                }
//
//                                @Override
//                                public void cancel() { }
//                            };
//                        }
//
//                        @Override
//                        public String getId(Object model) {
//                            return model.toString();
//                        }
//                    })
//                            .load(new Object())
//                            .animate(android.R.anim.fade_in)
//                            .centerCrop()
//                            .error(android.R.drawable.ic_delete)
//                            .placeholder(android.R.drawable.ic_dialog_email)
//                            .listener(new Glide.RequestListener<Object>() {
//                                @Override
//                                public void onException(Exception e, Object model, Target target) {
//                                    temp.invalidate();
//                                }
//
//                                @Override
//                                public void onImageReady(Object model, Target target) {
//                                    temp.jumpDrawablesToCurrentState();
//                                }
//                            })
//                            .into(temp);
//                    if (totalRun.getAndIncrement() < numRequests) {
//                        handler.postDelayed(this, 10);
//                    } else {
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                handlerThread.getLooper().quit();
//                            }
//                        }, 100);
//                    }
//                }
//        };
//        handler.post(doLoad);
//        handlerThread.join();
//    }
}

package com.bumptech.glide;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.ActivityTestCase;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.target.ImageViewTarget;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.presenter.target.Target;
import com.bumptech.glide.tests.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/25/13
 * Time: 12:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlideTest extends ActivityTestCase {
    private ImageView imageView;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        imageView = new ImageView(getInstrumentation().getContext());
        //this is a quick hack to get the SizeDeterminer in ImagePresenter to think the view has been measured
        imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
    }

    private void checkImagePresenter(Object model) {
        ImagePresenter imagePresenter = getImagePresenterFromView();
        imagePresenter.setModel(model);

        boolean caughtException = false;
        try {
            imagePresenter.setModel(new Float(4.4f));
        } catch (ClassCastException e) {
            caughtException = true;
        }

        assertTrue(caughtException);
    }

    private ImagePresenter getImagePresenterFromView() {
        return ((ImageViewTarget) imageView.getTag()).getImagePresenter();
    }

    public void testFileDefaultLoader() {
        File file = new File("fake");
        Glide.load(file).into(imageView);
        checkImagePresenter(file);
    }

    public void testUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");
        Glide.load(url).into(imageView);
        checkImagePresenter(url);
    }

    public void testUriDefaultLoader() {
        Uri uri = Uri.fromFile(new File("Fake"));
        Glide.load(uri).into(imageView);
        checkImagePresenter(uri);
    }

    public void testStringDefaultLoader() {
        String string = "http://www.google.com";
        Glide.load(string).into(imageView);
        checkImagePresenter(string);
    }

    public void testIntegerDefaultLoader() {
        int integer = 1234;
        Glide.load(integer).into(imageView);
        checkImagePresenter(integer);
    }

    public void testGlideDoesNotReplaceIdenticalPresenters() {
        Glide.load("fake")
                .asIs()
                .centerCrop()
                .animate(android.R.anim.fade_in)
                .placeholder(com.bumptech.glide.tests.R.raw.ic_launcher)
                .error(com.bumptech.glide.tests.R.raw.ic_launcher)
                .into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        Glide.load("fake2")
                .asIs()
                .centerCrop()
                .animate(android.R.anim.fade_in)
                .placeholder(com.bumptech.glide.tests.R.raw.ic_launcher)
                .error(com.bumptech.glide.tests.R.raw.ic_launcher)
                .into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        assertSame(first, second);
    }

    public void testCanHandleWrapContent() {
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Glide.load("fake").into(imageView);

        assertNotNull(getImagePresenterFromView());
    }

    public void testCanHandleWrapContentMatchParent() {
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        Glide.load("fake").into(imageView);

        assertNotNull(getImagePresenterFromView());
    }

    public void testDifferentModelTypesReplacesPresenters() {
        assertDifferentPresenters(
                Glide.load(4),
                Glide.load("fake")
        );
    }

    public void testDifferentModelLoadersReplacesPresenter() {
        ModelLoader<Object> first = new ModelLoader<Object>() {
            @Override
            public StreamLoader getStreamLoader(Object model, int width, int height) {
                return new StreamLoader() {
                    @Override
                    public void loadStream(StreamReadyCallback cb) {
                    }

                    @Override
                    public void cancel() {
                    }
                };
            }

            @Override
            public String getId(Object model) {
                return model.toString();
            }

        };

        ModelLoader<Object> second = new ModelLoader<Object>() {
            @Override
            public StreamLoader getStreamLoader(Object model, int width, int height) {
                return new StreamLoader() {
                    @Override
                    public void loadStream(StreamReadyCallback cb) {
                    }

                    @Override
                    public void cancel() {
                    }
                };
            }

            @Override
            public String getId(Object model) {
                return model.toString();
            }
        };

        final Object object = new Object();
        assertDifferentPresenters(
                Glide.using(first).load(object),
                Glide.using(second).load(object)
        );
    }

    public void testDifferentDownsamplersReplacesPresenter() {
        assertDifferentPresenters(
                Glide.load("a").approximate(),
                Glide.load("a").asIs()
        );
    }

    public void testDifferentTransformationsReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.load(file).centerCrop().fitCenter(),
                Glide.load(file).centerCrop()
        );
    }

    public void testDifferentPlaceholdersReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.load(file).placeholder(com.bumptech.glide.tests.R.raw.ic_launcher),
                Glide.load(file)

        );
    }

    public void testDifferentAnimationsReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.load(file).animate(android.R.anim.fade_in),
                Glide.load(file).animate(android.R.anim.fade_out)
        );
    }

    public void testDifferentErrorIdsReplacesPresenter() {
        assertDifferentPresenters(
                Glide.load("b").error(R.raw.ic_launcher),
                Glide.load("b").error(android.R.drawable.btn_star)
        );
    }

    public void testClearingTagReplacesPresenter() {
        Glide.load("a").into(imageView);
        assertNotNull(imageView.getTag());
        imageView.setTag(null);
        Glide.load("b").into(imageView);
    }

    public void testObtainAndOfferToBitmapPool() {
        Bitmap small = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap large = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
        BitmapPool bitmapPool = Glide.get().getImageManager(getInstrumentation().getContext()).getBitmapPool();
        bitmapPool.put(small);
        bitmapPool.put(large);

        assertEquals(small, bitmapPool.get(small.getWidth(), small.getHeight(), small.getConfig()));
        assertEquals(large, bitmapPool.get(large.getWidth(), large.getHeight(), large.getConfig()));
    }

    public void testThrowExceptionIfTagReplaced() {
        imageView.setTag(1234);
        Exception exception = null;
        try {
            Glide.load("a").into(imageView);
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
    }

    public void testDifferentRequestListenersReplacesPresenter() {
        assertDifferentPresenters(
                Glide.load("a").listener(new Glide.RequestListener<String>() {

                    @Override
                    public void onException(Exception e, String model, Target target) {

                    }

                    @Override
                    public void onImageReady(String model, Target target) {
                    }
                }),
                Glide.load("a").listener(new Glide.RequestListener<String>() {
                    @Override
                    public void onException(Exception e, String model, Target target) {
                    }

                    @Override
                    public void onImageReady(String model, Target target) {
                    }
                })
        );
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
//                        public StreamLoader getStreamLoader(Object model, int width, int height) {
//                            return new StreamLoader() {
//                                @Override
//                                public void loadStream(StreamReadyCallback cb) {
//                                    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
//                                    ByteArrayOutputStream os = new ByteArrayOutputStream();
//                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
//                                    cb.onStreamReady(new ByteArrayInputStream(os.toByteArray()));
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

    private void assertDifferentPresenters(Glide.Request a, Glide.Request b) {
        a.into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        a.into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        b.into(imageView);
        ImagePresenter third = getImagePresenterFromView();

        assertSame(first, second);
        assertNotSame(first, third);
    }
}

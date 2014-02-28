package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.test.ActivityTestCase;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.target.ImageViewTarget;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.presenter.target.Target;
import com.bumptech.glide.tests.R;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

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
        return getInstrumentation().getContext();
    }

    private void checkImagePresenter(Target target, Object model) {
        ImagePresenter imagePresenter = target.getImagePresenter();
        imagePresenter.setModel(model);

        boolean caughtException = false;
        try {
            imagePresenter.setModel(new Float(4.4f));
        } catch (ClassCastException e) {
            caughtException = true;
        }

        assertTrue(caughtException);
    }

    public void testFileDefaultLoader() {
        File file = new File("fake");
        Target target = Glide.with(getContext()).load(file).into(imageViewTarget);
        checkImagePresenter(target, file);
    }

    public void testUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");
        Target target = Glide.with(getContext()).load(url).into(imageViewTarget);
        checkImagePresenter(target, url);
    }

    public void testUriDefaultLoader() {
        Uri uri = Uri.fromFile(new File("Fake"));
        Target target = Glide.with(getContext()).load(uri).into(imageViewTarget);
        checkImagePresenter(target, uri);
    }

    public void testStringDefaultLoader() {
        String string = "http://www.google.com";
        Target target = Glide.with(getContext()).load(string).into(imageViewTarget);
        checkImagePresenter(target, string);
    }

    public void testIntegerDefaultLoader() {
        int integer = 1234;
        Target target = Glide.with(getContext()).load(integer).into(imageViewTarget);
        checkImagePresenter(target, integer);
    }

    public void testGlideDoesNotReplaceIdenticalPresenters() {
        Target target = Glide.with(getContext())
                .load("fake")
                .asIs()
                .centerCrop()
                .animate(android.R.anim.fade_in)
                .placeholder(com.bumptech.glide.tests.R.raw.ic_launcher)
                .error(com.bumptech.glide.tests.R.raw.ic_launcher)
                .into(imageViewTarget);
        ImagePresenter first = target.getImagePresenter();

        Target target2 = Glide.with(getContext())
                .load("fake2")
                .asIs()
                .centerCrop()
                .animate(android.R.anim.fade_in)
                .placeholder(com.bumptech.glide.tests.R.raw.ic_launcher)
                .error(com.bumptech.glide.tests.R.raw.ic_launcher)
                .into(imageViewTarget);
        ImagePresenter second = target2.getImagePresenter();

        assertSame(first, second);
    }

    public void testCanHandleWrapContent() {
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Target target = Glide.with(getContext()).load("fake").into(imageViewTarget);

        assertNotNull(target.getImagePresenter());
    }

    public void testCanHandleWrapContentMatchParent() {
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        Target target = Glide.with(getContext()).load("fake").into(imageViewTarget);

        assertNotNull(target.getImagePresenter());
    }

    public void testDifferentModelTypesReplacesPresenters() {
        assertDifferentPresenters(
                Glide.with(getContext()).load(4),
                Glide.with(getContext()).load("fake")
        );
    }

    public void testDifferentModelLoadersReplacesPresenter() {
        StreamModelLoader<Object> first = new StreamModelLoader<Object>() {

            @Override
            public ResourceFetcher<InputStream> getResourceFetcher(Object model, int width, int height) {
                return new ResourceFetcher<InputStream>() {
                    @Override
                    public InputStream loadResource() throws Exception {
                        return null;
                    }

                    @Override
                    public String getId() {
                        return null;
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

        StreamModelLoader<Object> second = new StreamModelLoader<Object>() {
            @Override
            public ResourceFetcher<InputStream> getResourceFetcher(Object model, int width, int height) {
                return new ResourceFetcher<InputStream>() {
                    @Override
                    public InputStream loadResource() throws Exception {
                        return null;
                    }

                    @Override
                    public String getId() {
                        return null;
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
                Glide.with(getContext()).using(first).load(object),
                Glide.with(getContext()).using(second).load(object)
        );
    }

    public void testDifferentDownsamplersReplacesPresenter() {
        assertDifferentPresenters(
                Glide.with(getContext()).load("a").approximate(),
                Glide.with(getContext()).load("a").asIs()
        );
    }

    public void testDifferentTransformationsReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.with(getContext()).load(file).centerCrop().fitCenter(),
                Glide.with(getContext()).load(file).centerCrop()
        );
    }

    public void testDifferentPlaceholdersReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.with(getContext()).load(file).placeholder(com.bumptech.glide.tests.R.raw.ic_launcher),
                Glide.with(getContext()).load(file)

        );
    }

    public void testDifferentAnimationsReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.with(getContext()).load(file).animate(android.R.anim.fade_in),
                Glide.with(getContext()).load(file).animate(android.R.anim.fade_out)
        );
    }

    public void testDifferentErrorIdsReplacesPresenter() {
        assertDifferentPresenters(
                Glide.with(getContext()).load("b").error(R.raw.ic_launcher),
                Glide.with(getContext()).load("b").error(android.R.drawable.btn_star)
        );
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

    public void testDifferentRequestListenersReplacesPresenter() {
        assertDifferentPresenters(
                Glide.with(getContext()).load("a").listener(new Glide.RequestListener<String>() {

                    @Override
                    public void onException(Exception e, String model, Target target) {

                    }

                    @Override
                    public void onImageReady(String model, Target target) {
                    }
                }),
                Glide.with(getContext()).load("a").listener(new Glide.RequestListener<String>() {
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

    private void assertDifferentPresenters(Glide.Request a, Glide.Request b) {
        ImagePresenter first = a.into(imageViewTarget).getImagePresenter();
        ImagePresenter second = a.into(imageViewTarget).getImagePresenter();
        ImagePresenter third = b.into(imageViewTarget).getImagePresenter();

        assertSame(first, second);
        assertNotSame(first, third);
    }
}

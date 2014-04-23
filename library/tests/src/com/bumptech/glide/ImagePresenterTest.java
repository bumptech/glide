package com.bumptech.glide;

import android.R;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.test.AndroidTestCase;
import android.view.animation.Animation;
import com.bumptech.glide.loader.bitmap.BitmapLoadFactory;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.image.ImageManagerLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.target.BaseTarget;
import com.bumptech.glide.presenter.target.Target;
import com.bumptech.glide.resize.BitmapLoad;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for the {@link ImagePresenter} class.
 */
public class ImagePresenterTest extends AndroidTestCase {

    private MockTarget target;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        target = new MockTarget();
    }

    public void testCanSetErrorDrawable() {
        new ImagePresenter.Builder()
                .setErrorDrawable(new ColorDrawable(Color.RED));
    }

    public void testCanSetErrorResource() {
        new ImagePresenter.Builder()
                .setErrorResource(R.drawable.btn_star);
    }

    public void testCantSetErrorResourceAndDrawable() {
        Exception exception = null;
        try {
            new ImagePresenter.Builder()
                    .setErrorDrawable(new ColorDrawable(Color.RED))
                    .setErrorResource(R.drawable.btn_star);
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
        exception = null;
        try {
            new ImagePresenter.Builder()
                    .setErrorResource(R.drawable.btn_star)
                    .setErrorDrawable(new ColorDrawable(Color.RED));
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
    }

    public void testMustProvideATarget() {
        Exception exception = null;
        try {
            new ImagePresenter.Builder<Object, Target>()
                    .setBitmapLoadFactory(new MockBitmapLoadFactory())
                    .build();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    public void testMustProvideAModelLoader() {
        Exception exception = null;
        try {
            new ImagePresenter.Builder<Object, Target>()
                    .setTarget(target, getContext())
                    .setImageLoader(new MockImageLoader())
                    .build();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    public void testMustProvideAnImageLoader() {
        Exception exception = null;
        try {
            new ImagePresenter.Builder<Object, Target>()
                    .setTarget(target, getContext())
                    .setBitmapLoadFactory(new MockBitmapLoadFactory())
                    .build();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    public void testPlaceholderIsSetWithNullModel() {
        Drawable placeholder = new ColorDrawable(Color.RED);
        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new ImageManagerLoader(getContext()))
                .setPlaceholderDrawable(placeholder)
                .build();
        imagePresenter.setModel(null);
        assertEquals(placeholder, target.getPlaceholder());
    }

    public void testPlaceholderIsSetWithNullBitmapLoadTask() {
        Drawable placeholder = new ColorDrawable(Color.RED);
        final Bitmap result = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory() {
                    @Override
                    public BitmapLoad getLoadTask(Object model, int width, int height) {
                        return null;
                    }
                })
                .setImageLoader(new MockImageLoader().onCallback(new MockImageLoader.CallbackAction() {
                    @Override
                    public void onCallbackReceived(ImageLoader.ImageReadyCallback cb) {
                        cb.onImageReady(result);
                    }
                }))
                .setPlaceholderDrawable(placeholder)
                .build();

        imagePresenter.setModel(new Object());
        assertEquals(placeholder, target.getPlaceholder());
    }

    public void testPlaceholderIsSetDuringAsynchronousLoad() {
        Drawable placeholder = new ColorDrawable(Color.RED);
        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new MockImageLoader())
                .setPlaceholderDrawable(placeholder)
                .build();

        imagePresenter.setModel(new Object());
        assertEquals(placeholder, target.getPlaceholder());
    }

    public void testPlaceholderIsNotSetDuringSynchronousLoad() {
        Drawable placeholder = new ColorDrawable(Color.RED);
        final Bitmap result = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444);

        final AtomicBoolean wasPlaceholderEverSet = new AtomicBoolean(false);
        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(new MockTarget() {
                    @Override
                    public void setPlaceholder(Drawable placeholder) {
                        wasPlaceholderEverSet.set(true);
                        super.setPlaceholder(placeholder);
                    }
                }, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new MockImageLoader().onCallback(new MockImageLoader.CallbackAction() {
                    @Override
                    public void onCallbackReceived(ImageLoader.ImageReadyCallback cb) {
                        cb.onImageReady(result);
                    }
                }))
                .setPlaceholderDrawable(placeholder)
                .build();

        imagePresenter.setModel(new Object());
        assertFalse(wasPlaceholderEverSet.get());
    }

    public void testErrorPlaceholderIsSetOnException() {
        Drawable errorDrawable = new ColorDrawable(Color.RED);
        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new MockImageLoader().onCallback(new MockImageLoader.CallbackAction() {
                    @Override
                    public void onCallbackReceived(ImageLoader.ImageReadyCallback cb) {
                        cb.onException(new Exception("Test"));
                    }
                }))
                .setErrorDrawable(errorDrawable)
                .build();

        imagePresenter.setModel(new Object());
        assertEquals(errorDrawable, target.getPlaceholder());
    }

    public void testBitmapIsSetWhenLoaded() {
        final Bitmap result = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new MockImageLoader().onCallback(new MockImageLoader.CallbackAction() {
                    @Override
                    public void onCallbackReceived(ImageLoader.ImageReadyCallback cb) {
                        cb.onImageReady(result);
                    }
                }))
                .build();
        imagePresenter.setModel(new Object());
        assertEquals(result, target.getBitmap());
    }

    public void testOldLoadsAreIgnoredIfNewFinishFirst() {
        final Bitmap first = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        final Bitmap second = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444);

        final List<ImageLoader.ImageReadyCallback> cbs = new ArrayList<ImageLoader.ImageReadyCallback>();
        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new MockImageLoader().onCallback(new MockImageLoader.CallbackAction() {
                    @Override
                    public void onCallbackReceived(ImageLoader.ImageReadyCallback cb) {
                        cbs.add(cb);
                    }
                }))
                .build();

        imagePresenter.setModel(new Object());
        imagePresenter.setModel(new Object());

        cbs.get(1).onImageReady(second);
        cbs.get(0).onImageReady(first);
        assertEquals(second, target.getBitmap());
    }

    public void testExceptionHandlerIsCalledOnException() {
        final Exception expected = new Exception("Test");
        final Object actualModel = new Object();

        final AtomicBoolean wasOnExceptionCalled = new AtomicBoolean(false);
        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new MockImageLoader().onCallback(new MockImageLoader.CallbackAction() {
                    @Override
                    public void onCallbackReceived(ImageLoader.ImageReadyCallback cb) {
                        cb.onException(expected);
                    }
                }))
                .setExceptionHandler(new ImagePresenter.ExceptionHandler<Object>() {
                    @Override
                    public void onException(Exception e, Object model, boolean isCurrent) {
                        assertEquals(expected, e);
                        assertEquals(actualModel, model);
                        wasOnExceptionCalled.set(true);
                    }
                })
                .build();

        imagePresenter.setModel(actualModel);

        assertTrue(wasOnExceptionCalled.get());
    }

    public void testSettingSameModelTwiceInARowIsNoOp() {
        final Object model = new Object();
        final AtomicInteger timesMockImageLoaderCalled = new AtomicInteger();

        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new MockImageLoader().onCallback(new MockImageLoader.CallbackAction() {
                    @Override
                    public void onCallbackReceived(ImageLoader.ImageReadyCallback cb) {
                        timesMockImageLoaderCalled.incrementAndGet();
                    }
                }))
                .build();

        imagePresenter.setModel(model);
        imagePresenter.setModel(model);

        assertEquals(1, timesMockImageLoaderCalled.get());
    }

    public void testImageSetCallbackIsCalledWhenImageIsSet() {
        final AtomicBoolean wasImageSetCallbackCalled = new AtomicBoolean(false);

        ImagePresenter<Object, Target> imagePresenter = new ImagePresenter.Builder<Object, Target>()
                .setTarget(target, getContext())
                .setBitmapLoadFactory(new MockBitmapLoadFactory())
                .setImageLoader(new MockImageLoader().onCallback(new MockImageLoader.CallbackAction() {
                    @Override
                    public void onCallbackReceived(ImageLoader.ImageReadyCallback cb) {
                        cb.onImageReady(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444));
                    }
                }))
                .setImageReadyCallback(new ImagePresenter.ImageReadyCallback<Object, Target>() {
                    @Override
                    public void onImageReady(Object model, Target target, boolean isFromMemoryCache) {
                        wasImageSetCallbackCalled.set(true);
                    }
                })
                .build();

        imagePresenter.setModel(new Object());
        assertTrue(wasImageSetCallbackCalled.get());
    }

    private static class MockTarget extends BaseTarget {

        private Bitmap bitmap = null;
        private Drawable placeholder = null;

        public Bitmap getBitmap() {
            return bitmap;
        }

        public Drawable getPlaceholder() {
            return placeholder;
        }

        @Override
        public void onImageReady(Bitmap bitmap) {
            placeholder = null;
            this.bitmap = bitmap;
        }

        @Override
        public void setPlaceholder(Drawable placeholder) {
            bitmap = null;
            this.placeholder = placeholder;
        }

        @Override
        public void getSize(SizeReadyCallback cb) {
            cb.onSizeReady(1, 1);
        }

        @Override
        public void startAnimation(Animation animation) {
        }
    }

    private static class MockImageLoader implements ImageLoader {
        private CallbackAction action;

        public interface CallbackAction {
            public void onCallbackReceived(ImageReadyCallback cb);
        }

        public MockImageLoader onCallback(CallbackAction action) {
            this.action = action;
            return this;
        }

        @Override
        public Object fetchImage(BitmapLoad loadTask, ImageReadyCallback cb) {
            if (action != null) {
                action.onCallbackReceived(cb);
            }
            return null;
        }

        @Override
        public void clear() { }
    }

    private static class MockBitmapLoadFactory implements BitmapLoadFactory<Object> {
        @Override
        public BitmapLoad getLoadTask(final Object model, int width, int height) {
            return new BitmapLoad() {
                @Override
                public String getId() {
                    return model.toString();
                }

                @Override
                public void cancel() {
                }

                @Override
                public Bitmap load(BitmapPool bitmapPool) throws Exception {
                    return null;
                }
            };
        }
    }
}

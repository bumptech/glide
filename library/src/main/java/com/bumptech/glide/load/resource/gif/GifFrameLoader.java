package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;

public class GifFrameLoader implements ModelLoader<GifDecoder, GifDecoder> {
    private String gifId;

    //TODO: this should be in the model.
    public GifFrameLoader(String gifId) {
        this.gifId = gifId;
    }

    @Override
    public DataFetcher<GifDecoder> getResourceFetcher(GifDecoder model, int width, int height) {
        return new GifFrameDataFetcher(model);
    }

    @Override
    public String getId(GifDecoder model) {
        return gifId + model.getCurrentFrameIndex();
    }

    private static class GifFrameDataFetcher implements DataFetcher<GifDecoder> {
        private GifDecoder decoder;

        public GifFrameDataFetcher(GifDecoder decoder) {
            this.decoder = decoder;
        }

        @Override
        public GifDecoder loadData(Priority priority) throws Exception {
            return decoder;
        }

        @Override
        public void cleanup() { }

        @Override
        public void cancel() { }
    }
}

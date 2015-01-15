package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.util.Preconditions;

import java.util.Arrays;
import java.util.List;

public class LoadPath<Data, ResourceType, Transcode> {
    private Class<Data> dataClass;
    private final List<? extends DecodePath<Data, ResourceType, Transcode>> decodePaths;

    public LoadPath(Class<Data> dataClass, List<DecodePath<Data, ResourceType, Transcode>>  decodePaths) {
        this.dataClass = dataClass;
        this.decodePaths = Preconditions.checkNotEmpty(decodePaths);
    }

    public Resource<Transcode> load(DataFetcher<Data> fetcher, RequestContext<ResourceType, Transcode> context,
            int width, int height, DecodePath.DecodeCallback<ResourceType> decodeCallback) throws Exception {
        Data data = fetcher.loadData(context.getPriority());
        if (data == null) {
            return null;
        }

        Resource<Transcode> result = null;
        DataRewinder<Data> rewinder = context.getRewinder(data);
        try {
            for (DecodePath<Data, ResourceType, Transcode> path : decodePaths) {
                result = path.decode(rewinder, width, height, decodeCallback);
                if (result != null) {
                    break;
                }
            }
        } finally {
            rewinder.cleanup();
        }
        return result;
    }

    public Class<Data> getDataClass() {
        return dataClass;
    }

    @Override
    public String toString() {
        return "LoadPath{"
                + "decodePaths=" + Arrays.toString(decodePaths.toArray(new DecodePath[decodePaths.size()]))
                + '}';
    }
}

package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * For a given {@link com.bumptech.glide.load.data.DataFetcher} for a given data class, attempts to fetch the data and
 * then run it through one or more {@link com.bumptech.glide.load.engine.DecodePath}s.
 *
 * @param <Data> The type of data that will be fetched.
 * @param <ResourceType> The type of intermediate resource that will be decoded within one of the
 * {@link com.bumptech.glide.load.engine.DecodePath}s.
 * @param <Transcode> The type of resource that will be returned as the result if the load and one of the
 *                   decode paths succeeds..
 */
public class LoadPath<Data, ResourceType, Transcode> {
    private final Class<Data> dataClass;
    private final Class<ResourceType> resourceClass;
    private final Class<Transcode> transcodeClass;
    private final List<? extends DecodePath<Data, ResourceType, Transcode>> decodePaths;

    public LoadPath(Class<Data> dataClass, Class<ResourceType> resourceClass, Class<Transcode> transcodeClass,
            List<DecodePath<Data, ResourceType, Transcode>> decodePaths) {
        this.dataClass = dataClass;
        this.resourceClass = resourceClass;
        this.transcodeClass = transcodeClass;
        this.decodePaths = Preconditions.checkNotEmpty(decodePaths);
    }

    public Resource<Transcode> load(DataFetcher<Data> fetcher, RequestContext<Transcode> context, int width, int height,
            DecodePath.DecodeCallback<ResourceType> decodeCallback) throws Exception {
        context.getDebugger().appendStartLoadPath(fetcher);
        final Data data;
        try {
            data = fetcher.loadData(context.getPriority());
        } catch (Exception e) {
            context.getDebugger().appendEndLoadPath(e);
            return null;
        }
        if (data == null) {
            context.getDebugger().appendEndLoadPath((Exception) null /*exception*/);
            return null;
        }
        context.getDebugger().appendLoadPathData(data);
        Resource<Transcode> result = null;
        DataRewinder<Data> rewinder = context.getRewinder(data);
        Map<String, Object> options = context.getOptions();
        try {
            for (DecodePath<Data, ResourceType, Transcode> path : decodePaths) {
                result = path.decode(rewinder, width, height, options, decodeCallback);
                context.getDebugger().appendDecodePath(path, result);
                if (result != null) {
                    break;
                }
            }
        } finally {
            rewinder.cleanup();
            fetcher.cleanup();
        }
        context.getDebugger().appendEndLoadPath(result);
        return result;
    }

    public Class<Data> getDataClass() {
        return dataClass;
    }

    public String getDebugString() {
        return "[" + dataClass + "->" + resourceClass + "->" + transcodeClass + "]";
    }

    public List<String> getDecodePathDebugStrings() {
        List<String> result = new ArrayList<String>();
        for (DecodePath<?, ?, ?> path : decodePaths) {
            result.add(path.getDebugString());
        }
        return result;
    }

    @Override
    public String toString() {
        return "LoadPath{"
                + "decodePaths=" + Arrays.toString(decodePaths.toArray(new DecodePath[decodePaths.size()]))
                + '}';
    }
}

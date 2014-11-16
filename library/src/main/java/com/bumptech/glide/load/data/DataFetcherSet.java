package com.bumptech.glide.load.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataFetcherSet {

    private List<DataFetcher<?>> fetchers = new ArrayList<DataFetcher<?>>();

    public boolean isEmpty() {
        return fetchers.isEmpty();
    }

    public String getId() {
        // TODO: can we be more intelligent?
        return fetchers.isEmpty() ? null : fetchers.get(0).getId();
    }

    public List<DataFetcher<?>> getFetchers() {
        return Collections.unmodifiableList(fetchers);
    }

    public void add(DataFetcher<?> fetcher) {
        fetchers.add(fetcher);
    }

    public void cancel() {
        for (DataFetcher<?> fetcher : fetchers) {
            fetcher.cancel();
        }
    }
}

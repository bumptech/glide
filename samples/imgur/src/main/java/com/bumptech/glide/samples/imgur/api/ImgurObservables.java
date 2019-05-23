package com.bumptech.glide.samples.imgur.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

/** Observables for retrieving metadata from Imgur's API. */
final class ImgurObservables {

  private final ImgurService imgurService;

  ImgurObservables(ImgurService imgurService) {
    this.imgurService = imgurService;
  }

  Observable<List<Image>> getHotViralImages(@SuppressWarnings("SameParameterValue") int maxPages) {
    return Observable.range(0, maxPages)
        .flatMap(
            new Func1<Integer, Observable<List<Image>>>() {
              @Override
              public Observable<List<Image>> call(Integer integer) {
                return imgurService
                    .getHotViral(integer)
                    .map(new GetData())
                    .flatMap(
                        new Func1<List<Image>, Observable<List<Image>>>() {
                          @Override
                          public Observable<List<Image>> call(List<Image> images) {
                            for (Iterator<Image> iterator = images.iterator();
                                iterator.hasNext(); ) {
                              if (iterator.next().is_album) {
                                iterator.remove();
                              }
                            }
                            return Observable.just(images);
                          }
                        });
              }
            })
        .takeWhile(
            new Func1<List<Image>, Boolean>() {
              @Override
              public Boolean call(List<Image> images) {
                return !images.isEmpty();
              }
            })
        .scan(
            new Func2<List<Image>, List<Image>, List<Image>>() {
              @Override
              public List<Image> call(List<Image> images, List<Image> images2) {
                List<Image> result = new ArrayList<>(images.size() + images2.size());
                result.addAll(images);
                result.addAll(images2);
                return result;
              }
            })
        .cache();
  }

  private static class GetData implements Func1<Gallery, List<Image>> {
    @Override
    public List<Image> call(Gallery gallery) {
      return gallery.data;
    }
  }
}

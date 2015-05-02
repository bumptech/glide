package com.bumptech.glide.load.data.mediastore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

interface ThumbnailQuery {
  Cursor query(Context context, Uri uri);
}

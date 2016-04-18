package com.bumptech.glide.load.data;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Fetches an {@link java.io.InputStream} for a local {@link android.net.Uri}.
 */
public class StreamLocalUriFetcher extends LocalUriFetcher<InputStream> {
    /** A lookup uri (e.g. content://com.android.contacts/contacts/lookup/3570i61d948d30808e537) */
    private static final int ID_LOOKUP = 1;
    /** A contact thumbnail uri (e.g. content://com.android.contacts/contacts/38/photo) */
    private static final int ID_THUMBNAIL = 2;
    /** A contact uri (e.g. content://com.android.contacts/contacts/38) */
    private static final int ID_CONTACT = 3;
    /**
     * A contact display photo (high resolution) uri
     * (e.g. content://com.android.contacts/5/display_photo)
     */
    private static final int ID_DISPLAY_PHOTO = 4;


    private static final UriMatcher URI_MATCHER;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*/#", ID_LOOKUP);
        URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*", ID_LOOKUP);
        URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#/photo", ID_THUMBNAIL);
        URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#", ID_CONTACT);
        URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#/display_photo", ID_DISPLAY_PHOTO);
    }

    public StreamLocalUriFetcher(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected InputStream loadResource(Uri uri, ContentResolver contentResolver) throws FileNotFoundException {
        final int matchedUri  = URI_MATCHER.match(uri);
        return loadResourceFromUri(uri, contentResolver, matchedUri);
    }

    @Override
    protected void close(InputStream data) throws IOException {
        data.close();
    }

    private InputStream loadResourceFromUri(Uri uri, ContentResolver contentResolver, int matchedUri)
            throws FileNotFoundException {
        switch (matchedUri) {
            case ID_LOOKUP:
            case ID_CONTACT:
                // If it was a Lookup uri then resolve it first, then continue loading the contact uri.
                if (matchedUri == ID_LOOKUP) {
                    uri = ContactsContract.Contacts.lookupContact(contentResolver, uri);
                    if (uri == null) {
                        throw new FileNotFoundException("Contact cannot be found");
                    }
                }
                return openContactPhotoInputStream(contentResolver, uri);
            case ID_THUMBNAIL:
            case ID_DISPLAY_PHOTO:
            case UriMatcher.NO_MATCH:
            default:
                return contentResolver.openInputStream(uri);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private InputStream openContactPhotoInputStream(ContentResolver contentResolver, Uri contactUri) {
        if (SDK_INT < ICE_CREAM_SANDWICH) {
            return ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactUri);
        } else {
            return ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactUri,
                    true /*preferHighres*/);
        }
    }
}

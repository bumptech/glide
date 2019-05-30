package com.bumptech.glide.load.data;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/** Fetches an {@link java.io.InputStream} for a local {@link android.net.Uri}. */
public class StreamLocalUriFetcher extends LocalUriFetcher<InputStream> {
  /** A lookup uri (e.g. content://com.android.contacts/contacts/lookup/3570i61d948d30808e537) */
  private static final int ID_CONTACTS_LOOKUP = 1;
  /** A contact thumbnail uri (e.g. content://com.android.contacts/contacts/38/photo) */
  private static final int ID_CONTACTS_THUMBNAIL = 2;
  /** A contact uri (e.g. content://com.android.contacts/contacts/38) */
  private static final int ID_CONTACTS_CONTACT = 3;
  /**
   * A contact display photo (high resolution) uri (e.g.
   * content://com.android.contacts/5/display_photo)
   */
  private static final int ID_CONTACTS_PHOTO = 4;
  /**
   * Uri for optimized search of phones by number (e.g.
   * content://com.android.contacts/phone_lookup/232323232
   */
  private static final int ID_LOOKUP_BY_PHONE = 5;
  /** Match the incoming Uri for special cases which we can handle nicely. */
  private static final UriMatcher URI_MATCHER;

  static {
    URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*/#", ID_CONTACTS_LOOKUP);
    URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*", ID_CONTACTS_LOOKUP);
    URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#/photo", ID_CONTACTS_THUMBNAIL);
    URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#", ID_CONTACTS_CONTACT);
    URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#/display_photo", ID_CONTACTS_PHOTO);
    URI_MATCHER.addURI(ContactsContract.AUTHORITY, "phone_lookup/*", ID_LOOKUP_BY_PHONE);
  }

  public StreamLocalUriFetcher(ContentResolver resolver, Uri uri) {
    super(resolver, uri);
  }

  @Override
  protected InputStream loadResource(Uri uri, ContentResolver contentResolver)
      throws FileNotFoundException {
    InputStream inputStream = loadResourceFromUri(uri, contentResolver);
    if (inputStream == null) {
      throw new FileNotFoundException("InputStream is null for " + uri);
    }
    return inputStream;
  }

  private InputStream loadResourceFromUri(Uri uri, ContentResolver contentResolver)
      throws FileNotFoundException {
    switch (URI_MATCHER.match(uri)) {
      case ID_CONTACTS_CONTACT:
        return openContactPhotoInputStream(contentResolver, uri);
      case ID_CONTACTS_LOOKUP:
      case ID_LOOKUP_BY_PHONE:
        // If it was a Lookup uri then resolve it first, then continue loading the contact uri.
        uri = ContactsContract.Contacts.lookupContact(contentResolver, uri);
        if (uri == null) {
          throw new FileNotFoundException("Contact cannot be found");
        }
        return openContactPhotoInputStream(contentResolver, uri);
      case ID_CONTACTS_THUMBNAIL:
      case ID_CONTACTS_PHOTO:
      case UriMatcher.NO_MATCH:
      default:
        return contentResolver.openInputStream(uri);
    }
  }

  private InputStream openContactPhotoInputStream(ContentResolver contentResolver, Uri contactUri) {
    return ContactsContract.Contacts.openContactPhotoInputStream(
        contentResolver, contactUri, true /*preferHighres*/);
  }

  @Override
  protected void close(InputStream data) throws IOException {
    data.close();
  }

  @NonNull
  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }
}

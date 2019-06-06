package com.bumptech.glide.samples.contacturi;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Preconditions;

/**
 * An activity that demonstrates loading photos using {@link
 * com.bumptech.glide.load.data.StreamLocalUriFetcher content uris} through Glide. It works by
 * making the user to choose a contact when presses a button, and after he chooses a contact with
 * photo, We try to load both a high res image and thumbnail image of that contact with various
 * Uris.
 */
public class MainActivity extends Activity {
  private static final int REQUEST_CONTACT = 1;
  private static final int READ_CONTACTS = 0;

  private ImageView imageViewContact;
  private ImageView imageViewLookup;
  private ImageView imageViewPhoto;
  private ImageView imageViewDisplayPhoto;
  private EditText numberEntry;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    imageViewContact = findViewById(R.id.image_contact);
    imageViewLookup = findViewById(R.id.image_lookup);
    imageViewPhoto = findViewById(R.id.image_photo);
    imageViewDisplayPhoto = findViewById(R.id.image_display_photo);
    numberEntry = findViewById(R.id.number_entry);
    // Make sure that user gives application required permissions
    if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_CONTACTS)
        != PackageManager.PERMISSION_GRANTED) {
      // No explanation needed, we can request the permission.
      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.READ_CONTACTS}, READ_CONTACTS);
    }

    findViewById(R.id.button_pick_contact)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
                startActivityForResult(intent, REQUEST_CONTACT);
              }
            });

    findViewById(R.id.button_find)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                Uri uri =
                    Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(numberEntry.getText().toString()));
                GlideApp.with(MainActivity.this)
                    .load(uri)
                    .override(Target.SIZE_ORIGINAL)
                    .into(imageViewLookup);
              }
            });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CONTACT && resultCode == RESULT_OK) {
      Uri uri = Preconditions.checkNotNull(data.getData());
      final Cursor cursor = getContentResolver().query(uri, null, null, null, null);
      try {
        if (cursor != null && cursor.moveToFirst()) {
          final long contactId = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
          showContact(contactId);
        }
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void showContact(long id) {
    GlideRequests glideRequests = GlideApp.with(this);
    RequestOptions originalSize = new RequestOptions().override(Target.SIZE_ORIGINAL);

    Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, id);
    glideRequests.load(contactUri).apply(originalSize).into(imageViewContact);

    Uri lookupUri = Contacts.getLookupUri(getContentResolver(), contactUri);
    glideRequests.load(lookupUri).apply(originalSize).into(imageViewLookup);

    Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
    glideRequests.load(photoUri).apply(originalSize).into(imageViewPhoto);

    Uri displayPhotoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.DISPLAY_PHOTO);
    glideRequests.load(displayPhotoUri).apply(originalSize).into(imageViewDisplayPhoto);
  }
}

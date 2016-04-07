package com.bumptech.glide.samples.contacturi;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

/**
 * An activity that demonstrates loading photos using content uris through Glide.
 * It works by making the user to choose a contact when presses a button, and after he chooses a contact with photo,
 * We try to load both a high res image and thumbnail image of that contact.
 */
public class MainActivity extends Activity {
    private static final int REQUEST_CONTACT = 1;

    private ImageView imageViewHighRes;
    private ImageView imageViewThumbnail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewHighRes = (ImageView) findViewById(R.id.image_highres);
        imageViewThumbnail = (ImageView) findViewById(R.id.image_thumbnail);

        findViewById(R.id.button_pick_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, REQUEST_CONTACT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONTACT && resultCode == RESULT_OK) {
            final Cursor cursor =
                    getContentResolver().query(data.getData(), null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    // Get ID of the contact
                    final long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                    final Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
                    final Uri thumbnailPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo
                            .CONTENT_DIRECTORY);

                    Glide.with(this).load(contactUri).into(imageViewHighRes);
                    Glide.with(this).load(thumbnailPhotoUri).into(imageViewThumbnail);
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
}

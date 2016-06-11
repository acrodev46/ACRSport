package com.example.testproject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.romgou.acrsport.R;
import com.microsoft.azure.storage.analytics.StorageService;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class Report extends ListActivity {
    private StorageService mStorageService;
    private final String TAG = "BlobsActivity";
    private String mContainerName;
    private ImageView mImgBlobImage;
    private Uri mImageUri;
    private AlertDialog mAlertDialog;

    Button btnSelect, btnR;
    ImageView iv;
    TextView tv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get access to the storage service
        StorageApplication myApp = (StorageApplication) getApplication();
        mStorageService = myApp.getStorageService();
        //Get data from the intent that launched this activity
        Intent launchIntent = getIntent();
        mContainerName = launchIntent.getStringExtra("ContainerName");

        //Get the blobs for the selected container
        mStorageService.getBlobsForContainer(mContainerName);


        setContentView(R.layout.camera);
        tv1=(TextView) findViewById(R.id.editText1);
        btnR = (Button)findViewById(R.id.btnReport);
        iv = (ImageView) findViewById(R.id.imageView1);
        btnSelect = (Button)findViewById(R.id.btnSelect);
        //Set select image handler
        btnSelect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

    }

    public void onClick(View v) {
        mStorageService.getSasForNewBlob(mContainerName, tv1.getText().toString());

    }

    // Fire off intent to select image from gallery
    protected void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1111);
    }

    // Result handler for any intents started with startActivityForResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            //handle result from gallary select
            if (requestCode == 1111) {
                Uri currImageURI = data.getData();
                mImageUri = currImageURI;
                //Set the image view's image by using imageUri
                mImgBlobImage.setImageURI(currImageURI);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    /***
     * Handles uploading an image to a specified url
     */
    class ImageUploaderTask extends AsyncTask<Void, Void, Boolean> {
        private String mUrl;
        public ImageUploaderTask(String url) {
            mUrl = url;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                //Get the image data
                Cursor cursor = getContentResolver().query(mImageUri, null,null, null, null);
                cursor.moveToFirst();
                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                String absoluteFilePath = cursor.getString(index);
                FileInputStream fis = new FileInputStream(absoluteFilePath);
                int bytesRead = 0;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] b = new byte[1024];
                while ((bytesRead = fis.read(b)) != -1) {
                    bos.write(b, 0, bytesRead);
                }
                byte[] bytes = bos.toByteArray();
                // Post our image data (byte array) to the server
                URL url = new URL(mUrl.replace("\"", ""));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("PUT");
                urlConnection.addRequestProperty("Content-Type", "image/jpeg");
                urlConnection.setRequestProperty("Content-Length", ""+ bytes.length);
                // Write image data to server
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.write(bytes);
                wr.flush();
                wr.close();
                int response = urlConnection.getResponseCode();
                //If we successfully uploaded, return true
                if (response == 201
                        && urlConnection.getResponseMessage().equals("Created")) {
                    return true;
                }
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean uploaded) {
            if (uploaded) {
                mAlertDialog.cancel();
                mStorageService.getBlobsForContainer(mContainerName);
            }
        }
    }
}
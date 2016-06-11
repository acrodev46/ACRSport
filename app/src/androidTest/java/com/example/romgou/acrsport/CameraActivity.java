package com.example.romgou.acrsport;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.romgou.acrsport.R;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by romgou on 10/05/2016.
 */
public class CameraActivity extends Activity {

    Button camera;
    ImageView image;
    Bitmap bp;
    EditText numeroPhoto;
    Button upload;

    final int REQUEST_IMAGE_CAPTURE = 1;

    String mCurrentPhotoPath;

    Uri currImageURI;

    public static final String storageConnectionString = "DefaultEndpointsProtocol=https;"
            + "AccountName=acrsportstorage;"
            + "AccountKey=73rkfcw3DDymb5wPNQlOCLytsP1QhsFENP7SHCYX3iQfYYMf57eX2C4v/Mai3f8d8jzyBgFVhgMzBLEmIkC8GA==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);
        camera=(Button)findViewById(R.id.b_camera);
        image=(ImageView)findViewById(R.id.iv_image);
        numeroPhoto = (EditText) findViewById(R.id.et_numeroPhoto);
        upload = (Button) findViewById(R.id.b_upload);
        numeroPhoto.setVisibility(View.VISIBLE);
        upload.setVisibility(View.VISIBLE);


        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            //Here starts the code for Azure Storage Blob
                            try {
                                // Retrieve storage account from connection-string
                                CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                                // Create the blob client
                                CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                                // Get a reference to a container
                                // The container name must be lower case
                                CloudBlobContainer container = blobClient.getContainerReference("mycontainer");

                                // Create the container if it does not exist
                                container.createIfNotExists();

                                // Create a permissions object
                                BlobContainerPermissions containerPermissions = new BlobContainerPermissions();

                                // Include public access in the permissions object
                                containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);

                                // Set the permissions on the container
                                container.uploadPermissions(containerPermissions);

                                // Create or overwrite the "myimage.jpg" blob with contents from a local file
                                CloudBlockBlob blob = container.getBlockBlobReference("myimage.jpg");
                                File source = new File(currImageURI.toString());
                                blob.upload(new FileInputStream(source), source.length());
                                blob.upl
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            bp = (Bitmap) extras.get("data");
            image.setImageBitmap(bp);
            try {
                createImageFile();
                galleryAddPic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
}

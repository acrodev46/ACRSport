package com.example.romgou.acrsport.Camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.romgou.acrsport.CreateQueue;
import com.example.romgou.acrsport.R;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.ByteArrayOutputStream;


/**
 * Created by romgou on 10/05/2016.
 */
public class CameraActivity extends Activity {

    Button camera;
    ImageView image;
    Bitmap bp;
    EditText numeroPhoto;
    Button upload;
    TextView textNumeroEquipe;

    int REQUEST_IMAGE_CAPTURE = 1;
    String NOM_POSITION = null;
    int NUMERO_EQUIPE = 0;

    boolean isUploading = false;

    public static final String storageConnectionString = "DefaultEndpointsProtocol=https;"
            + "AccountName=acrsportstorage;"
            + "AccountKey=73rkfcw3DDymb5wPNQlOCLytsP1QhsFENP7SHCYX3iQfYYMf57eX2C4v/Mai3f8d8jzyBgFVhgMzBLEmIkC8GA==";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        camera = (Button) findViewById(R.id.b_camera);
        image = (ImageView) findViewById(R.id.iv_image);
        numeroPhoto = (EditText) findViewById(R.id.et_numeroPhoto);
        upload = (Button) findViewById(R.id.b_upload);
        textNumeroEquipe = (TextView) findViewById(R.id.tv_numeroEquipeCamera);

        NOM_POSITION = (String) getIntent().getSerializableExtra("nomPosition");

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
                if (!isUploading)
                    Upload();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CreateQueue task = new CreateQueue(this);
        task.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            bp = (Bitmap) extras.get("data");
            image.setImageBitmap(bp);

            numeroPhoto.setVisibility(View.VISIBLE);
            upload.setVisibility(View.VISIBLE);
            textNumeroEquipe.setVisibility(View.VISIBLE);

        }
    }

    void Upload() {
        if (bp != null) {
            isUploading = true;
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            final byte[] byteArray = stream.toByteArray();

            NUMERO_EQUIPE = Integer.parseInt(numeroPhoto.getText().toString());

            //Here starts the code for Azure Storage Blob
            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        // Retrieve storage account from connection-string
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                        // Create the blob client
                        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                        // Get a reference to a container
                        // The container name must be lower case
                        CloudBlobContainer container = blobClient.getContainerReference("photosequipe");

                        // Create the container if it does not exist
                        container.createIfNotExists();

                        // Create a permissions object
                        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();

                        // Include public access in the permissions object
                        containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);

                        // Set the permissions on the container
                        container.uploadPermissions(containerPermissions);

                        // Create or overwrite the "myimage.jpg" blob with contents from a local file
                        CloudBlockBlob blob = container.getBlockBlobReference(NUMERO_EQUIPE + " - " + NOM_POSITION + ".jpg");

                        blob.uploadFromByteArray(byteArray, 0, byteArray.length);

                        Log.i("Fin Asynctask", "Upload terminé ");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean b) {
                    super.onPostExecute(b);
                    if (b) {
                        MakeToast("Upload Terminé !");
                        bp = null;
                        numeroPhoto.setVisibility(View.INVISIBLE);
                        upload.setVisibility(View.INVISIBLE);
                        textNumeroEquipe.setVisibility(View.INVISIBLE);
                        image.setImageResource(R.mipmap.ic_runner);
                        isUploading = false;
                        return;
                    } else {
                        MakeToast("Upload Echoué, Verifiez votre connection Internet");
                        return;
                    }
                }
            };
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    private void MakeToast (final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
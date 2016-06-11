package com.example.romgou.acrsport;

import android.app.Activity;
import android.os.AsyncTask;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import com.microsoft.windowsazure.mobileservices.table.query.Query;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by romgou on 12/05/2016.
 */

public class CreateQueue extends AsyncTask<Void, Void, Void> {

    private static UUID idUser;
    private Activity act;
    public static final String storageConnectionString = "DefaultEndpointsProtocol=https;"
            + "AccountName=acrsportstoragequeue;"
            + "AccountKey=sKL6easJdTNOPYeV3B3p2PBPzGrRdYL05ulPU834nH6KqYB5MFT6pQfy9qkGKLtzVnK1LUrsmMt9DpO3GbXZpw==";
    public static CloudQueueMessage lastMessage;
    public static final String deviceQueue = "connecteddevice";


    public CreateQueue(Activity act){
        this.act = act;
        if (idUser == null)
            idUser = UUID.randomUUID();
    }

    @Override
    protected Void doInBackground(Void... params) {

        try {

            CloudStorageAccount account = CloudStorageAccount
                    .parse(storageConnectionString);

            CloudQueueClient queueClient = account.createCloudQueueClient();

            CloudQueue queue = queueClient.getQueueReference(deviceQueue + idUser);

            queue.createIfNotExists();

            String actName = act.getLocalClassName();

            CloudQueueMessage message = new CloudQueueMessage(actName);
            queue.addMessage(message);

        } catch (StorageException | URISyntaxException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void DeleteQueue()
    {
        CloudStorageAccount account = null;
        try {
            account = CloudStorageAccount
                    .parse(storageConnectionString);
            // Create a queue service client
            CloudQueueClient queueClient = account.createCloudQueueClient();

            CloudQueue queue = queueClient.getQueueReference(deviceQueue + idUser);
            queue.deleteIfExists();

        } catch (URISyntaxException | InvalidKeyException | StorageException e) {
            e.printStackTrace();
        }
    }


}

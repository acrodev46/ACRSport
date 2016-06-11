package com.example.romgou.acrsport.Main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.romgou.acrsport.ChoixPosition.ChoixPositionActivity;
import com.example.romgou.acrsport.CreateQueue;
import com.example.romgou.acrsport.MyHandler;
import com.example.romgou.acrsport.NotificationSettings;
import com.example.romgou.acrsport.R;
import com.example.romgou.acrsport.RegistrationIntentService;
import com.example.romgou.acrsport.TablesClass.Equipe;
import com.example.romgou.acrsport.TempsEquipe.TempsEquipeActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOperations;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {


    //Permet la connection au service Azure App
    private MobileServiceClient mClient;
    private MobileServiceSyncTable<Equipe> tableEquipe;
    private EquipeAdapter eAdapter;
    private boolean isShowAll = false;
    private boolean isSync = false;

    public static MainActivity mainActivity;
    public static Boolean isVisible = false;
    private GoogleCloudMessaging gcm;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainActivity = this;
        NotificationsManager.handleNotifications(this, NotificationSettings.SenderId, MyHandler.class);
        registerWithNotificationHubs();


        //Connection au service Azure, création de la table Mobile service
        // mise en place de l'adapter pour la list view
        try {
            mClient = new MobileServiceClient("https://acrsport.azurewebsites.net", this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        authenticate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
        showAll(getCurrentFocus());
        CreateQueue task = new CreateQueue(this);
        task.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CreateQueue task = new CreateQueue(this);
        task.DeleteQueue();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                sync();
                showAll(null);
                return true;
        }
        return false;
    }

    public void showAll(View view) {
        isShowAll = true;
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    do
                        Thread.sleep(2000);
                    while (isSync);
                    Query query = QueryOperations.field("numero").orderBy("numero", QueryOrder.Ascending).ne(0);
                    final List<Equipe> results = tableEquipe.read(query).get();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            eAdapter.clear();
                            for (Equipe item : results) {
                                eAdapter.add(item);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isShowAll = false;
                return null;
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private AsyncTask<Void, Void, Void> sync() {
        isSync = true;
        if (isNetworkAvailable()) {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        MobileServiceSyncContext syncContext = mClient.getSyncContext();
                        Thread.sleep(2000);
                        syncContext.push().get();
                        tableEquipe.purge(null);
                        Thread.sleep(1000);
                        tableEquipe.pull(null);
                        ToastNotify("Synchronisation réussie !");
                    } catch (final Exception e) {
                        e.printStackTrace();
                        ToastNotify("Erreur de Synchronisation au serveur");
                    }
                    isSync = false;
                    return null;
                }

            };
            return task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        } else {
            ToastNotify ("Vous êtes déconnecté, Synchronisation échouée!");
            isSync = false;
            return null;
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private AsyncTask<Void, Void, Void> initLocalStore() throws MobileServiceLocalStoreException, ExecutionException, InterruptedException {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    MobileServiceSyncContext syncContext = mClient.getSyncContext();

                    if (syncContext.isInitialized())
                        return null;

                    publishProgress();
                    SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "OfflineStore", null, 1);

                    Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("numero", ColumnDataType.Integer);
                    tableDefinition.put("abandon", ColumnDataType.Boolean);

                    localStore.defineTable("Equipe", tableDefinition);
                    publishProgress();
                    SimpleSyncHandler handler = new SimpleSyncHandler();

                    syncContext.initialize(localStore, handler).get();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }
        };

        return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void authenticate() {
        // We first try to load a token cache if one exists.
        if (loadUserTokenCache(mClient)) {
            createTable();
        }
        // If we failed to load a token cache, login and create a token cache
        else {
            // Login using the Google provider.
            ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.WindowsAzureActiveDirectory);

            Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                @Override
                public void onFailure(Throwable exc) {
                    ToastNotify ("Vous devez vous connecter !");
                }

                @Override
                public void onSuccess(final MobileServiceUser user) {
                    ToastNotify (String.format("Vous etes connectés en tant que %1$2s", user.getUserId()));
                    cacheUserToken(mClient.getCurrentUser());
                    createTable();
                }
            });
        }
    }

    private void createTable() {

        try {
            tableEquipe = mClient.getSyncTable("Equipe", Equipe.class);
            Log.i("LocalStore", "Initialisation du local store");
            initLocalStore().get();
            eAdapter = new EquipeAdapter(this, R.layout.ligne_numero);
            final ListView listViewEquipe = (ListView) findViewById(R.id.lv_listNumero);
            if (listViewEquipe != null) {
                listViewEquipe.setAdapter(eAdapter);
            } else {
                Log.i("Erreur", "Adapter bug");
            }

            listViewEquipe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Equipe e = (Equipe) listViewEquipe.getItemAtPosition(position);
                    Intent intentListeTemps = new Intent(MainActivity.this, TempsEquipeActivity.class);
                    intentListeTemps.putExtra("idEquipe", e.getId().toString());
                    startActivity(intentListeTemps);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button boutonSuivi = (Button) findViewById(R.id.b_suiviTemps);
        boutonSuivi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isShowAll && !isSync) {
                    Intent intentChoixPosition = new Intent(MainActivity.this, ChoixPositionActivity.class);
                    startActivity(intentChoixPosition);
                }
            }
        });
        sync();
        showAll(null);
    }



    private void cacheUserToken(MobileServiceUser user) {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.commit();
    }

    private boolean loadUserTokenCache(MobileServiceClient client) {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");
        if (userId == "undefined")
            return false;
        String token = prefs.getString(TOKENPREF, "undefined");
        if (token == "undefined")
            return false;

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i("", "This device is not supported by Google Play Services.");
                ToastNotify("This device is not supported by Google Play Services.");
                finish();
            }
            return false;
        }
        return true;
    }

    public void registerWithNotificationHubs() {
        Log.i("", " Registering with Notification Hubs");

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    public void ToastNotify(final String notificationMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, notificationMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

}
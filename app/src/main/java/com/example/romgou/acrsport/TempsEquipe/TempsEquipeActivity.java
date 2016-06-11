package com.example.romgou.acrsport.TempsEquipe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.romgou.acrsport.CreateQueue;
import com.example.romgou.acrsport.NotificationSettings;
import com.example.romgou.acrsport.R;
import com.example.romgou.acrsport.TablesClass.Equipe;
import com.example.romgou.acrsport.TablesClass.PointChrono;
import com.example.romgou.acrsport.TablesClass.SuiviTemps;
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by romgou on 28/04/2016.
 */
public class TempsEquipeActivity extends AppCompatActivity {

    //Permet la connection au service Azure App
    private MobileServiceClient mClient;
    private MobileServiceSyncTable<SuiviTemps> tableSuivi;
    private MobileServiceSyncTable<Equipe> tableEquipe;
    private MobileServiceSyncTable<PointChrono> tablePoint;
    private TempsEquipeAdapter teAdapter;
    private String idEquipe;
    private boolean isSync = false;
    private int numeroEquipe;
    private boolean isAbandon = false;

    private String HubEndpoint = null;
    private String HubSasKeyName = null;
    private String HubSasKeyValue = null;

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.liste_temps_equipe);

        //Recuperation de l'id de l'equipe cliquée
        idEquipe = (String) getIntent().getSerializableExtra("idEquipe");

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
    protected void onResume() {
        super.onResume();
        CreateQueue task = new CreateQueue(this);
        task.execute();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                sync();
                return true;
        }
        return false;
    }

    public void showAll(View view) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    do
                        Thread.sleep(2000);
                    while (isSync);
                    Query query = QueryOperations.field("id").eq(idEquipe.toUpperCase());
                    final List<Equipe> resultEquipe = tableEquipe.read(query).get();
                    numeroEquipe = resultEquipe.get(0).getNumero();
                    final TextView tv_numeroEquipe = (TextView) findViewById(R.id.tv_numeroEquipe);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_numeroEquipe.setText("Equipe " + numeroEquipe);
                        }
                    });
                    query = QueryOperations.field("equipeId").orderBy("datePassage", QueryOrder.Ascending).eq(idEquipe);
                    final List<SuiviTemps> resultsSuivi = tableSuivi.read(query).get();
                    final List<SuiviTempsPosition> suiviTempsPosition = new ArrayList<SuiviTempsPosition>();
                    List<PointChrono> resultPoint;
                    int i = 0;
                    for (SuiviTemps item : resultsSuivi) {
                        query = QueryOperations.field("id").eq(item.getPointChronoId().toString().toUpperCase());
                        resultPoint = tablePoint.read(query).get();
                        suiviTempsPosition.add(new SuiviTempsPosition(item));
                        suiviTempsPosition.get(i).setNomPosition(resultPoint.get(0).getNomPosition());
                        i++;
                    }

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            teAdapter.clear();
                            for (SuiviTempsPosition item : suiviTempsPosition) {

                                teAdapter.add(item);
                            }
                        }
                    });
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return null;
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class Abandon extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Query query = QueryOperations.field("id").eq(idEquipe.toUpperCase());
                final List<Equipe> resultsEquipe = tableEquipe.read(query).get();
                resultsEquipe.get(0).setAbandon(!resultsEquipe.get(0).isAbandon());
                isAbandon = resultsEquipe.get(0).isAbandon();
                numeroEquipe = resultsEquipe.get(0).getNumero();
                tableEquipe.update(resultsEquipe.get(0));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
                        if (resultsEquipe.get(0).isAbandon())
                            toast.setText("L'equipe Abandonne");
                        else
                            toast.setText("L'equipe reprends la course");
                        toast.show();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            sync();
            sendNotificationButtonOnClick(getCurrentFocus());
            super.onPostExecute(aVoid);
        }
    }

    public class SuiviTempsPosition extends SuiviTemps {

        private String nomPosition;

        public SuiviTempsPosition(SuiviTemps s) {
            super(s);
        }

        public String getNomPosition() {
            return nomPosition;
        }

        public void setNomPosition(String nomPosition) {
            this.nomPosition = nomPosition;
        }
    }

    private AsyncTask<Void, Void, Void> sync() {
        if (isNetworkAvailable()) {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        isSync = true;
                        MobileServiceSyncContext syncContext = mClient.getSyncContext();
                        syncContext.push().get();
                        Thread.sleep(1000);
                        tableEquipe.purge(null);
                        tablePoint.purge(null);
                        tableSuivi.purge(null);
                        tableEquipe.pull(null);
                        tablePoint.pull(null);
                        tableSuivi.pull(null);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(TempsEquipeActivity.this, "Synchronisation réussie !" +
                                        "", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } catch (final Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(TempsEquipeActivity.this, "Erreur de Synchronisation au serveur" +
                                        "", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    isSync = false;
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    showAll(null);
                }
            };
            return task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TempsEquipeActivity.this, "Vous êtes déconnecté, Synchronisation échouée!" +
                            "", Toast.LENGTH_SHORT).show();
                }
            });
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

                    SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "OfflineStore", null, 1);

                    Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("numero", ColumnDataType.Integer);
                    tableDefinition.put("abandon", ColumnDataType.Boolean);

                    localStore.defineTable("Equipe", tableDefinition);

                    tableDefinition.clear();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("ordre", ColumnDataType.Integer);
                    tableDefinition.put("nomPosition", ColumnDataType.String);

                    localStore.defineTable("PointChrono", tableDefinition);

                    tableDefinition.clear();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("datePassage", ColumnDataType.Date);
                    tableDefinition.put("equipeId", ColumnDataType.String);
                    tableDefinition.put("pointChronoId", ColumnDataType.String);
                    tableDefinition.put("userId", ColumnDataType.String);

                    localStore.defineTable("SuiviTemps", tableDefinition);

                    SimpleSyncHandler handler = new SimpleSyncHandler();

                    syncContext.initialize(localStore, handler).get();

                } catch (Exception e) {
                    Throwable t = e;
                    while (t.getCause() != null) {
                        t = t.getCause();
                    }
                    final AlertDialog.Builder builder = new AlertDialog.Builder(TempsEquipeActivity.this);

                    builder.setMessage("Unknown error: " + t.getMessage());
                    builder.setTitle("Error");
                    builder.create().show();
                }

                return null;
            }
        };

        return task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    private void createTable() {
        try {
            initLocalStore();
            tableSuivi = mClient.getSyncTable(SuiviTemps.class);
            tableEquipe = mClient.getSyncTable(Equipe.class);
            tablePoint = mClient.getSyncTable(PointChrono.class);
            teAdapter = new TempsEquipeAdapter(this, R.layout.ligne_liste_temps_equipe);
            ListView listViewEquipe = (ListView) findViewById(R.id.lv_listTempsEquipe);
            listViewEquipe.setAdapter(teAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button retour = (Button) findViewById(R.id.b_retourMain);
        if (retour != null) {
            retour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        Button abandon = (Button) findViewById(R.id.b_abandon);
        if (abandon != null) {
            abandon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Abandon().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                }
            });
        }
        showAll(getCurrentFocus());
    }

    private void authenticate() {
        // We first try to load a token cache if one exists.
        if (loadUserTokenCache(mClient))
        {
            createTable();
        }
        // If we failed to load a token cache, login and create a token cache
        else
        {
            // Login using the Google provider.
            ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Google);

            Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                @Override
                public void onFailure(Throwable exc) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TempsEquipeActivity.this, "Vous devez vous connecter !", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void onSuccess(final MobileServiceUser user) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TempsEquipeActivity.this, "Vous etes maintenant connéctée : " + user.getUserId(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    cacheUserToken(mClient.getCurrentUser());
                    createTable();
                }
            });
        }
    }

    private void cacheUserToken(MobileServiceUser user)
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.commit();
    }

    private boolean loadUserTokenCache(MobileServiceClient client)
    {
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

    private void ParseConnectionString(String connectionString)
    {
        String[] parts = connectionString.split(";");
        if (parts.length != 3)
            throw new RuntimeException("Error parsing connection string: "
                    + connectionString);

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("Endpoint")) {
                this.HubEndpoint = "https" + parts[i].substring(11);
            } else if (parts[i].startsWith("SharedAccessKeyName")) {
                this.HubSasKeyName = parts[i].substring(20);
            } else if (parts[i].startsWith("SharedAccessKey")) {
                this.HubSasKeyValue = parts[i].substring(16);
            }
        }
    }

    private String generateSasToken(String uri) {

        String targetUri;
        String token = null;
        try {
            targetUri = URLEncoder
                    .encode(uri.toString().toLowerCase(), "UTF-8")
                    .toLowerCase();

            long expiresOnDate = System.currentTimeMillis();
            int expiresInMins = 60; // 1 hour
            expiresOnDate += expiresInMins * 60 * 1000;
            long expires = expiresOnDate / 1000;
            String toSign = targetUri + "\n" + expires;

            // Get an hmac_sha1 key from the raw key bytes
            byte[] keyBytes = HubSasKeyValue.getBytes("UTF-8");
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(toSign.getBytes("UTF-8"));

            // Using android.util.Base64 for Android Studio instead of
            // Apache commons codec
            String signature = URLEncoder.encode(
                    Base64.encodeToString(rawHmac, Base64.NO_WRAP).toString(), "UTF-8");

            // Construct authorization string
            token = "SharedAccessSignature sr=" + targetUri + "&sig="
                    + signature + "&se=" + expires + "&skn=" + HubSasKeyName;
        } catch (Exception e) {
            ToastNotify("Exception Generating SaS : " + e.getMessage().toString());
        }

        return token;
    }

    public void sendNotificationButtonOnClick(View v) {

        String message = null;
        if (isAbandon)
        {
            message = "{\"data\":{\"message\":\"" + numeroEquipe + " Abandonne\"}}";
        } else {
            message = "{\"data\":{\"message\":\"" + numeroEquipe + " reprends la course\"}}";
        }

        final String json = message;


        new Thread()
        {
            public void run()
            {
                try
                {
                    // Based on reference documentation...
                    // http://msdn.microsoft.com/library/azure/dn223273.aspx
                    ParseConnectionString(NotificationSettings.HubFullAccess);
                    URL url = new URL(HubEndpoint + NotificationSettings.HubName +
                            "/messages/?api-version=2015-01");

                    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

                    try {
                        // POST request
                        urlConnection.setDoOutput(true);

                        // Authenticate the POST request with the SaS token
                        urlConnection.setRequestProperty("Authorization",
                                generateSasToken(url.toString()));

                        // Notification format should be GCM
                        urlConnection.setRequestProperty("ServiceBusNotification-Format", "gcm");

                        // Include any tags
                        // Example below targets 3 specific tags
                        // Refer to : https://azure.microsoft.com/documentation/articles/notification-hubs-routing-tag-expressions/
                        // urlConnection.setRequestProperty("ServiceBusNotification-Tags",
                        //      "tag1 || tag2 || tag3");

                        // Send notification message
                        urlConnection.setFixedLengthStreamingMode(json.length());
                        OutputStream bodyStream = new BufferedOutputStream(urlConnection.getOutputStream());
                        bodyStream.write(json.getBytes());
                        bodyStream.close();

                        // Get reponse
                        urlConnection.connect();
                        int responseCode = urlConnection.getResponseCode();
                        if ((responseCode != 200) && (responseCode != 201)) {
                            BufferedReader br = new BufferedReader(new InputStreamReader((urlConnection.getErrorStream())));
                            String line;
                            StringBuilder builder = new StringBuilder("Send Notification returned " +
                                    responseCode + " : ")  ;
                            while ((line = br.readLine()) != null) {
                                builder.append(line);
                            }

                            ToastNotify(builder.toString());
                        }
                    } finally {
                        urlConnection.disconnect();
                    }
                }
                catch(Exception e)
                {
                        ToastNotify("Exception Sending Notification : " + e.getMessage().toString());
                }
            }
        }.start();
    }

    public void ToastNotify(final String notificationMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TempsEquipeActivity.this, notificationMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

}
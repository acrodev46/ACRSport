package com.example.romgou.acrsport.ChoixPosition;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.romgou.acrsport.R;
import com.example.romgou.acrsport.SuiviTemps.SuiviTempsActivity;
import com.example.romgou.acrsport.TablesClass.PointChrono;
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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by romgou on 29/04/2016.
 */
public class ChoixPositionActivity extends AppCompatActivity {

    private MobileServiceClient mClient;
    private MobileServiceSyncTable<PointChrono> tablePosition;
    private ArrayAdapter<PointChrono> pcAdapter;
    private boolean isShowAll = false;
    private boolean isSync = false;

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choix_position);

        try {
            mClient = new MobileServiceClient("https://acrsport.azurewebsites.net", this);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        authenticate();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                sync();
                return true;
        }
        return false;
    }

    public void showAll (View view)
    {
        isShowAll = true;
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    do
                        Thread.sleep(2000);
                    while (isSync);
                    Query query = QueryOperations.field("ordre").orderBy("ordre", QueryOrder.Ascending).ne(-1);
                    final List<PointChrono> resultsPosition = tablePosition.read(query).get();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pcAdapter.clear();
                            for (PointChrono p : resultsPosition)
                            {
                               pcAdapter.add(p);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
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
                        Thread.sleep(1000);
                        syncContext.push().get();
                        tablePosition.purge(null).get();
                        tablePosition.pull(null).get();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ChoixPositionActivity.this, "Syncronisation réussie !" +
                                        "", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ChoixPositionActivity.this, "Erreur de Synchronisation au serveur" +
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
        }else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ChoixPositionActivity.this, "Vous êtes déconnecté, Syncronisation échouée!" +
                            "", Toast.LENGTH_SHORT).show();
                }
            });
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

                    SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "OfflineStore", null, 1);

                    Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("ordre", ColumnDataType.Integer);
                    tableDefinition.put("nomPosition", ColumnDataType.String);

                    localStore.defineTable("PointChrono", tableDefinition);

                    SimpleSyncHandler handler = new SimpleSyncHandler();

                    syncContext.initialize(localStore, handler).get();

                } catch (Exception e) {
                    Throwable t = e;
                    while (t.getCause() != null) {
                        t = t.getCause();
                    }
                    final AlertDialog.Builder builder = new AlertDialog.Builder(ChoixPositionActivity.this);

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
        Spinner choixPosition = null;
        try {
            initLocalStore();
            tablePosition = mClient.getSyncTable(PointChrono.class);
            choixPosition = (Spinner) findViewById(R.id.s_choixPosition);
            pcAdapter = new ArrayAdapter<PointChrono>(this, android.R.layout.simple_spinner_item);
            pcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            choixPosition.setAdapter(pcAdapter);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (MobileServiceLocalStoreException e) {
            e.printStackTrace();
        }

        Button valider = (Button) findViewById(R.id.b_choixPosition);
        final Spinner finalChoixPosition = choixPosition;
        assert valider != null;
        valider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (finalChoixPosition != null && !isShowAll && !isSync)
                {
                    try {
                        PointChrono pointChronoChoisi = (PointChrono) finalChoixPosition.getSelectedItem();
                        Intent intentSuiviTemps = new Intent(ChoixPositionActivity.this, SuiviTempsActivity.class);
                        intentSuiviTemps.putExtra("pointChronoChoisi", pointChronoChoisi.getId().toString());
                        intentSuiviTemps.putExtra("nomPosition", pointChronoChoisi.getNomPosition());
                        startActivity(intentSuiviTemps);
                        finish();
                    } catch (NullPointerException e) {
                        e.fillInStackTrace();
                    }
                }
            }
        });

        showAll(null);
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
                            Toast.makeText(ChoixPositionActivity.this, "Vous devez vous connecter !", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void onSuccess(final MobileServiceUser user) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ChoixPositionActivity.this, "Vous etes maintenant connéctée : " + user.getUserId(), Toast.LENGTH_SHORT).show();
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



}

package com.example.romgou.acrsport.SuiviTemps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.romgou.acrsport.Camera.CameraActivity;
import com.example.romgou.acrsport.ChoixPosition.ChoixPositionActivity;
import com.example.romgou.acrsport.CreateQueue;
import com.example.romgou.acrsport.TablesClass.Equipe;
import com.example.romgou.acrsport.R;
import com.example.romgou.acrsport.TablesClass.GPSTracker;
import com.example.romgou.acrsport.TablesClass.PointChrono;
import com.example.romgou.acrsport.TablesClass.SuiviTemps;
import com.example.romgou.acrsport.TempsEquipe.TempsEquipeAdapter;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Striped;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOperations;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.val;

/**
 * Created by romgou on 26/04/2016.
 * Ajoute le temps des équipes en fonction de leurs numéro
 */
public class SuiviTempsActivity extends AppCompatActivity {

    private MobileServiceClient mClient;
    private MobileServiceSyncTable<SuiviTemps> tableSuivi;
    private SuiviTempsAdapter stAdapter;
    private MobileServiceSyncTable<Equipe> tableEquipe;
    private String idPosition;
    private ListView listViewSuivi;
    private GPSTracker gpsTracker;
    private boolean isShowAll = false;
    private boolean isSync = false;

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passage_equipe);

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
        getMenuInflater().inflate(R.menu.menu_photo, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                sync();
                return true;
            case R.id.menu_camera:
                Intent intentCamera = new Intent(SuiviTempsActivity.this, CameraActivity.class);
                intentCamera.putExtra("nomPosition", (String) getIntent().getSerializableExtra("nomPosition"));
                startActivity(intentCamera);
                return true;
        }
        return false;
    }

    // Fonction qui permet de rafraichir la liste
    public void showAll(View view) {
        isShowAll = true;
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    do
                        Thread.sleep(2000);
                    while (isSync);
                    Query query = QueryOperations.field("numero").orderBy("numero", QueryOrder.Ascending).ne(0);
                    final List<Equipe> resultsEquipe = tableEquipe.read(query).get();

                    query = QueryOperations.field("pointChronoId").orderBy("datePassage", QueryOrder.Ascending).eq(idPosition.toString());
                    final List<SuiviTemps> resultsSuivi = tableSuivi.read(query).get();

                    final int nbreEquipeTotal = resultsEquipe.size();
                    int nbreEquipePasse = resultsSuivi.size();
                    final String textCompteur = "" + nbreEquipePasse + "/" + nbreEquipeTotal;

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            stAdapter.clear();
                            TextView compteur = (TextView) findViewById(R.id.tv_compteur);
                            compteur.setText(textCompteur);

                            SuiviTempsEquipe itemSuiviTempsEquipe;
                            for (SuiviTemps item : resultsSuivi) {
                                if (!resultsEquipe.isEmpty())
                                {
                                    int i = 0;
                                    do {
                                        itemSuiviTempsEquipe = new SuiviTempsEquipe(item);
                                        itemSuiviTempsEquipe.setNumeroEquipe(resultsEquipe.get(i).getNumero());
                                        i++;
                                    }
                                    while (!item.getEquipeId().equals(resultsEquipe.get(i - 1).getId()) && i < nbreEquipeTotal);
                                    stAdapter.add(itemSuiviTempsEquipe);
                                }
                            }
                        }
                    });
                } catch (Exception exception) {
                    exception.fillInStackTrace();
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
                        syncContext.push().get();
                        Thread.sleep(1000);
                        tableSuivi.purge(null);
                        tableEquipe.purge(null);
                        tableEquipe.pull(null).get();
                        tableSuivi.pull(null).get();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SuiviTempsActivity.this, "Syncronisation réussie !" +
                                        "", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (final Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SuiviTempsActivity.this, "Erreur de Synchronisation au serveur" +
                                        "", Toast.LENGTH_SHORT).show();
                            }
                        });
                        e.printStackTrace();
                        clearApplicationData();
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
                    Toast.makeText(SuiviTempsActivity.this, "Vous êtes déconnecté, Syncronisation échouée!" +
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

    // Fonction qui ajoute le passage de l'equipe a la BDD
    private class AjoutEquipe extends AsyncTask<Integer, Void, Void> {

        UUID idEquipe = null;
        SuiviTemps suivi = null;

        @Override
        protected Void doInBackground(Integer... params) {
            try {
                Query query = QueryOperations.field("numero").eq(params[0]);
                final List<Equipe> results = tableEquipe.read(query).get();

                // Vérifie si le numéro d'équipe existe
                if (results.isEmpty())
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(SuiviTempsActivity.this)
                                    .setTitle("Erreur")
                                    .setMessage("Equipe Introuvable")
                                    .show();
                        }
                    });
                    return null;
                }

                // Vériofie que l'équipe n'ai pas abandonné
                if (results.get(0).isAbandon())
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(SuiviTempsActivity.this)
                                    .setTitle("Abandon")
                                    .setMessage("L'equipe a abandonné")
                                    .show();
                        }
                    });
                    return null;
                }

                query = QueryOperations.field("equipeId").eq(results.get(0).getId().toString()).and().field("pointChronoId").eq(idPosition.toString());
                final List<SuiviTemps> resultsSuivi = tableSuivi.read(query).get();

                // Vérifie que l'équipe ne soit pas déja passée
                if (!resultsSuivi.isEmpty())
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(SuiviTempsActivity.this)
                                    .setTitle("Erreur Equipe")
                                    .setMessage("L'equipe est deja passee")
                                    .show();
                        }
                    });
                    return null;
                }

                idEquipe = results.get(0).getId();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            suivi = new SuiviTemps();
            suivi.setEquipeId(idEquipe);
            Date d = new Date();
            suivi.setDatePassage(d);
            suivi.setPointChronoId(UUID.fromString(idPosition));
            suivi.setUserId(mClient.getCurrentUser().getUserId());
            suivi.setLongitude(String.valueOf(gpsTracker.getLongitude()));
            suivi.setLatitude(String.valueOf(gpsTracker.getLatitude()));

            // Instert le temps dans la table et affiche un message
            try {
                SuiviTemps result = tableSuivi.insert(suivi).get();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), "Temps Enregistre", Toast.LENGTH_SHORT);
                        toast.setText("Temps Enregistré");
                        toast.show();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            showAll(getCurrentFocus());
        }

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
                    tableDefinition.put("longitude", ColumnDataType.String);
                    tableDefinition.put("latitude", ColumnDataType.String);

                    localStore.defineTable("SuiviTemps", tableDefinition);

                    SimpleSyncHandler handler = new SimpleSyncHandler();

                    syncContext.initialize(localStore, handler).get();

                } catch (Exception e) {
                    Throwable t = e;
                    while (t.getCause() != null) {
                        t = t.getCause();
                    }
                    final AlertDialog.Builder builder = new AlertDialog.Builder(SuiviTempsActivity.this);

                    builder.setMessage("Unknown error: " + t.getMessage());
                    builder.setTitle("Error");
                    builder.create().show();
                }

                return null;
            }
        };

        return task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    protected class SuiviTempsEquipe extends SuiviTemps {
        private int numeroEquipe;

        public SuiviTempsEquipe (SuiviTemps s) {
            super(s);
        }

        public int getNumeroEquipe() {
            return numeroEquipe;
        }

        public void setNumeroEquipe(int numeroEquipe) {
            this.numeroEquipe = numeroEquipe;
        }
    }

    public void clearApplicationData() {
        File cache = getCacheDir();
        File appDir = new File(cache.getParent());
        if(appDir.exists()){
            String[] children = appDir.list();
            for(String s : children){
                if(!s.equals("lib")){
                    deleteDir(new File(appDir, s));
                    Log.i("TAG", "File /data/data/APP_PACKAGE/" + s +" DELETED");
                }
            }
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }

    private void createTable() {
        try {
            initLocalStore();
            tableSuivi = mClient.getSyncTable("SuiviTemps", SuiviTemps.class);
            stAdapter = new SuiviTempsAdapter(this, R.layout.ligne_passage_equipe);
            listViewSuivi = (ListView) findViewById(R.id.lv_listTemps);
            tableEquipe = mClient.getSyncTable("Equipe", Equipe.class);
            idPosition = (String) getIntent().getSerializableExtra("pointChronoChoisi");
            if (listViewSuivi != null) {
                listViewSuivi.setAdapter(stAdapter);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (MobileServiceLocalStoreException e) {
            e.printStackTrace();
        }

        Boolean isGPS = null;
        gpsTracker = new GPSTracker(this);
        if(gpsTracker.canGetLocation() ){
            gpsTracker.getLocation();
            isGPS = true;
        }else{
            isGPS = false;
        }
        Toast.makeText(SuiviTempsActivity.this, "isGPS = " + isGPS, Toast.LENGTH_SHORT).show();

        //Bouton permettant le retour a la liste des équipes
        Button retourEquipe = (Button) findViewById(R.id.b_retourListe);
        if (retourEquipe != null) {
            retourEquipe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // Bouton entrant les données de l'équipe associés a l'heure dans la BDD
        Button ajoutPassage = (Button) findViewById(R.id.b_saveTemps);
        if (ajoutPassage != null) {
            ajoutPassage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText editNumero;
                    String strNumeroAjout;
                    int numeroAjout;

                    editNumero = (EditText) findViewById(R.id.et_editNumeroEquipe);
                    strNumeroAjout = editNumero.getText().toString();

                    if (strNumeroAjout.length() <= 0)
                    {
                        new AlertDialog.Builder(SuiviTempsActivity.this)
                                .setTitle("Erreur")
                                .setMessage("Veuillez Remplir le champ")
                                .show();
                        return;
                    }

                    try {
                        numeroAjout = Integer.parseInt(strNumeroAjout);
                        new AjoutEquipe().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, numeroAjout);
                    } catch (NumberFormatException e) {
                        new AlertDialog.Builder(SuiviTempsActivity.this)
                                .setTitle("Erreur")
                                .setMessage("nombre trop grand")
                                .show();
                    }
                    editNumero.setText("");
                }
            });
        }

        if (listViewSuivi != null)
        {
            listViewSuivi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final SuiviTempsEquipe s = (SuiviTempsEquipe) listViewSuivi.getItemAtPosition(position);
                    AlertDialog.Builder supprAlert = new AlertDialog.Builder(SuiviTempsActivity.this);
                    supprAlert.setTitle("Suppression")
                            .setMessage("Etes vous sur de vouloir supprimer le temps de l'equipe " + s.getNumeroEquipe() + " ?");
                    supprAlert.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
                                @Override
                                protected Void doInBackground(Void... params) {
                                    tableSuivi.delete(s);
                                    return null;
                                }
                                @Override
                                protected void onPostExecute(Void aVoid) {
                                    super.onPostExecute(aVoid);
                                    showAll(getCurrentFocus());
                                }
                            };
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    });
                    supprAlert.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    supprAlert.show();
                }
            });
        }
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
                            Toast.makeText(SuiviTempsActivity.this, "Vous devez vous connecter !", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void onSuccess(final MobileServiceUser user) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SuiviTempsActivity.this, "Vous etes maintenant connéctée : " + user.getUserId(), Toast.LENGTH_SHORT).show();
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

package com.example.romgou.acrsport.TempsEquipe;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.romgou.acrsport.R;
import com.example.romgou.acrsport.TablesClass.SuiviTemps;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.text.SimpleDateFormat;

/**
 * Created by romgou on 28/04/2016.
 */
public class TempsEquipeAdapter extends ArrayAdapter<TempsEquipeActivity.SuiviTempsPosition> {

    public TempsEquipeAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        final TempsEquipeActivity.SuiviTempsPosition suiviActuel = getItem(position);


        if (row == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            row = inflater.inflate(R.layout.ligne_liste_temps_equipe, null);
        }

        row.setTag(suiviActuel);

        final TextView ligneLieu = (TextView) row.findViewById(R.id.tv_lignelisteLieuEquipe);
        ligneLieu.setText("" + suiviActuel.getNomPosition());

        // Entre la date
        final TextView ligneTemps = (TextView) row.findViewById(R.id.tv_lignelisteTempsEquipe);
        SimpleDateFormat formatDate = new SimpleDateFormat("HH:mm:ss  -  dd/MM/yyyy  -  z");
        ligneTemps.setText("" + formatDate.format(suiviActuel.getDatePassage()));

        return row;
    }
}

package com.example.romgou.acrsport.SuiviTemps;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.romgou.acrsport.R;
import com.example.romgou.acrsport.TablesClass.SuiviTemps;

import java.text.SimpleDateFormat;

public class SuiviTempsAdapter extends ArrayAdapter<SuiviTempsActivity.SuiviTempsEquipe> {

    public SuiviTempsAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        final SuiviTempsActivity.SuiviTempsEquipe suiviActuel = getItem(position);


        if (row == null)
        {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            row = inflater.inflate(R.layout.ligne_passage_equipe, parent, false);
        }

        row.setTag(suiviActuel);

        // Entre le numéro d'équipr sur la premiere ligne de la cellule
        final TextView ligneSuiviNumero = (TextView) row.findViewById(R.id.tv_ligneSuiviNumeroEquipe);
        ligneSuiviNumero.setText("Equipe " + suiviActuel.getNumeroEquipe());

        // Entre la date sur la seconde
        final TextView ligneSuivi = (TextView) row.findViewById(R.id.tv_ligneSuiviTempsEquipe);
        SimpleDateFormat formatDate = new SimpleDateFormat("HH:mm:ss  -  dd/MM/yyyy  -  z");
        ligneSuivi.setText("" + formatDate.format(suiviActuel.getDatePassage()));

        return row;
    }
}

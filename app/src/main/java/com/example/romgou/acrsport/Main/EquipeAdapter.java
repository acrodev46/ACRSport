package com.example.romgou.acrsport.Main;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.romgou.acrsport.R;
import com.example.romgou.acrsport.TablesClass.Equipe;

/**
 * Created by romgou on 26/04/2016.
 */
public class EquipeAdapter extends ArrayAdapter<Equipe> {

    public EquipeAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View ligne = convertView;

        final Equipe equipeActuelle = getItem(position);

        if (ligne == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            ligne = inflater.inflate(R.layout.ligne_numero, parent, false);
        }

        ligne.setTag(equipeActuelle);

        final TextView text = (TextView) ligne.findViewById(R.id.tv_textNumero);

        if (equipeActuelle.isAbandon())
        {
            text.setText("Equipe " + equipeActuelle.getNumero() + "  (Abandon)");
            text.setTextColor(Color.RED);
        } else {
            text.setText("Equipe " + equipeActuelle.getNumero());
            text.setTextColor(Color.GRAY);
            //text.setTextColor(Color.BLACK);
        }

        return ligne;
    }

}

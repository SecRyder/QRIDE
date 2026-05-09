package com.example.qride.login.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.qride.R;
import com.example.qride.login.model.Language;

import java.util.List;

public class LanguageAdapter extends ArrayAdapter<Language> {

    public LanguageAdapter(Context context, List<Language> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_spinner_login, parent, false);
        }

        Language item = getItem(position);

        ImageView imgFlag = convertView.findViewById(R.id.imgFlag);
        TextView tvLanguage = convertView.findViewById(R.id.tvLanguage);

        imgFlag.setImageResource(item.getFlagRes());
        tvLanguage.setText(item.getName());

        return convertView;
    }
}


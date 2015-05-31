package com.rdmills.Cataloguer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.parse.ParseObject;

import java.util.List;

/**
 * Created by Randy on 16/04/2015.
 */
public class CatalogueAdapter extends BaseAdapter {
    private List<ParseObject> data;

    /**
     *
     */
    public CatalogueAdapter(List<ParseObject> data) {
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    public List<ParseObject> getData() {
        return data;
    }

    @Override
    public ParseObject getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if(convertView == null) {
            @SuppressWarnings("static-access")
            LayoutInflater inflater = (LayoutInflater)parent.getContext().getSystemService(parent.getContext().LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_catalogue, null);
        }

        ParseObject catalogue = getItem(position);

        TextView catalogueTitle = (TextView) view.findViewById(R.id.tv_title);
        TextView typeText = (TextView) view.findViewById(R.id.tv_type);

        catalogueTitle.setText(catalogue.getString("title"));
        typeText.setText(catalogue.getString("type"));

        return view;
    }
}

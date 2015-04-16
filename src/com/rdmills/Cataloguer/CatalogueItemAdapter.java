package com.rdmills.Cataloguer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.parse.ParseObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by Randy on 16/04/2015.
 */
public class CatalogueItemAdapter extends BaseAdapter {

    private List<ParseObject> catalogueData;

    public CatalogueItemAdapter(List<ParseObject> data) {
        this.catalogueData = data;
    }

    public void sortData(int option) {
        this.catalogueData = sort(option,catalogueData);
    }

    @Override
    public int getCount() {
        return catalogueData.size();
    }

    @Override
    public ParseObject getItem(int position) {
        return catalogueData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if(view == null) {
            @SuppressWarnings("static-access")
            LayoutInflater inflater = (LayoutInflater)parent.getContext().getSystemService(parent.getContext().LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_book, parent, false);
        }

        ParseObject temp = getItem(position);

        ImageView iv_thumbnail = (ImageView) view.findViewById(R.id.iv_thumbnail);

        String thumbnail_url = temp.getString("thumbnail");

        if(!thumbnail_url.equals("")) {
            new DownloadImageTask(iv_thumbnail).execute(temp.getString("thumbnail"));
        } else {
            iv_thumbnail.setImageResource(R.drawable.no_image);
        }

        TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
        TextView tv_author = (TextView) view.findViewById(R.id.tv_author);

        tv_title.setText(temp.getString("title"));
        JSONArray authors = temp.getJSONArray("authors");
        String authorString = "";
        for(int i=0;i<authors.length();i++) {
            try {
                authorString = authorString + ", " + authors.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(authorString.length() > 2)
            authorString = authorString.substring(2);

        tv_author.setText(authorString);

        return view;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    public List<ParseObject> sort(int option, List<ParseObject> items) {
        List<ParseObject> answer = new ArrayList<ParseObject>();
        for(ParseObject p : items) {
            int pos = 0;
            boolean searching = true;
            if(answer.size() == 0) {
                answer.add(p);
            } else if(option == 0) {
                while(searching && pos < answer.size()) {
                    String c = p.getString("title");
                    String d = answer.get(pos).getString("title");
                    if(c.compareTo(d) < 1) {
                        answer.add(pos,p);
                        searching = false;
                    }
                    pos++;
                }

                if(searching)
                    answer.add(p);
            } else if(option == 1) {
                while(searching && pos < answer.size()) {
                    String c;
                    String d;
                    try {
                        c = p.getJSONArray("authors").getString(0);
                        d = answer.get(pos).getJSONArray("authors").getString(0);
                        if(c.compareTo(d) < 1) {
                            answer.add(pos,p);
                            searching = false;
                        }
                        pos++;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if(searching)
                    answer.add(p);
            }
        }
        return answer;
    }
}

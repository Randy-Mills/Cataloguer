package com.rdmills.Cataloguer;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Randy on 16/04/2015.
 */
public class DataFetcher extends AsyncTask<String, Void, JSONObject> {

    private String isbn;
    private String cId;
    private CatalogueActivity parent;

    public DataFetcher(CatalogueActivity parent) {
        this.parent = parent;
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        isbn = params[0];
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn" + isbn;
        cId = params[1];

        JSONObject result = new JSONObject();

        try {
            result = JsonReader.readJsonFromUrl(url);
        } catch (Exception e) {
            Log.d("Cataloguer", "Error: " + e);
        }

        return result;
    }

    protected void onPostExecute(JSONObject result) {
        try {
            JSONObject vi = result.getJSONArray("items").getJSONObject(0).getJSONObject("volumeInfo");

            final String title = vi.getString("title");

            String subtitle;
            try {
                subtitle = vi.getString("subtitle");
            } catch (JSONException je) {
                subtitle = "";
            }
            String image;
            try {
                image = (String)vi.getJSONObject("imageLinks").get("smallThumbnail");
            } catch (JSONException je) {
                image = "";
            }

            JSONArray authors;
            try {
                authors = vi.getJSONArray("authors");
            } catch (JSONException je) {
                authors = new JSONArray();
            }

            String publisher;
            try {
                publisher = vi.getString("publisher");
            } catch (JSONException je) {
                publisher = "";
            }

            String publishedDate;
            try {
                publishedDate = vi.getString("publishedDate");
            } catch (JSONException je) {
                publishedDate = "";
            }

            int pageCount;
            try {
                pageCount = vi.getInt("pageCount");
            } catch (JSONException je) {
                pageCount = 0;
            }

            Book book = new Book(title,subtitle,authors,image,isbn,publisher,publishedDate,pageCount);
            parent.confirmDialog(cId, book);
        } catch (Exception e) {
            Log.d("Cataloguer", "Error: " + e);
        }
    }
}

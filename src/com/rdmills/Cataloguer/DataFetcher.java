package com.rdmills.Cataloguer;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Randy on 16/04/2015.
 */
public class DataFetcher extends AsyncTask<String, Void, JSONObject> {

    private String isbn;
    private MyActivity parent;

    public DataFetcher(MyActivity parent) {
        this.parent = parent;
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        isbn = params[0];
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn" + isbn;

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
            JSONArray booksData = result.getJSONArray("items");
            JSONObject volumeInfo;
            String amazonId;

            String title;
            String subtitle;
            String image;
            JSONArray authors;
            String publisher;
            String publishedDate;
            int pageCount;
            String fullIsbn;

            Book[] books = new Book[booksData.length()];

            for(int i=0; i<booksData.length();i++) {
                amazonId = booksData.getJSONObject(i).getString("id");
                volumeInfo = booksData.getJSONObject(i).getJSONObject("volumeInfo");

                title = volumeInfo.getString("title");
                subtitle = volumeInfo.optString("subtitle");

                try {
                    image = volumeInfo.getJSONObject("imageLinks").optString("smallThumbnail");
                } catch (JSONException je) {
                    image = "";
                }

                try {
                    JSONArray identifiers = volumeInfo.getJSONArray("industryIdentifiers");
                    fullIsbn = identifiers.getJSONObject(0).optString("identifier") + "|" +
                            identifiers.getJSONObject(1).optString("identifier");
                } catch (JSONException je) {
                    fullIsbn = isbn;
                }

                authors = volumeInfo.optJSONArray("authors");
                publisher = volumeInfo.optString("publisher");
                publishedDate = volumeInfo.optString("publishedDate");
                pageCount = volumeInfo.optInt("pageCount");

                books[i] = new Book(title,subtitle,
                                    authors,image,
                                    fullIsbn, isbn, publisher,
                                    publishedDate,pageCount,
                                    amazonId);
            }

            parent.confirmDialog(books);
        } catch (JSONException e) {
            Log.d("Cataloguer", "JSONException: " + e.getMessage());
        } catch (Exception e) {
            Log.d("Cataloguer", "Exception: " + e.getMessage());
        }

    }
}

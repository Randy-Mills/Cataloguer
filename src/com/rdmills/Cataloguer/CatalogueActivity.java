package com.rdmills.Cataloguer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.parse.*;
import org.json.JSONArray;

import java.util.List;

/**
 * Created by Randy on 16/04/2015.
 */
public class CatalogueActivity extends Activity {

    private String catalogueId;
    private Intent detailActivity;
    private CatalogueItemAdapter catalogueItemAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalogue);

        detailActivity = new Intent(this, DetailsActivity.class);

        catalogueId = "";
        Bundle extra = this.getIntent().getExtras();
        if(extra != null) {
            catalogueId = extra.getString("cId");
        }

        fillList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillList();
    }

    private void fillList() {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Book");
        query.whereContains("catalogue_id", catalogueId)
             .whereEqualTo("active", true)
             .findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if(e == null) {
                    catalogueItemAdapter = new CatalogueItemAdapter(list);
                    ListView listView = (ListView) findViewById(R.id.lv_catalogueItems);
                    listView.setEmptyView(findViewById(R.id.rl_empty));
                    listView.setAdapter(catalogueItemAdapter);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            ParseObject temp = catalogueItemAdapter.getItem(position);
                            detailActivity.putExtra("title", temp.getString("title"));
                            detailActivity.putExtra("subtitle", temp.getString("subtitle"));
                            JSONArray authorFetch = temp.getJSONArray("authors");
                            String[] authors = new String[authorFetch.length()];
                            for(int i=0;i<authorFetch.length();i++) {
                                try {
                                    authors[i] = authorFetch.getString(i);
                                } catch (Exception e) {
                                    Log.d("Cataloguer", "Error retrieving authors: " + e.getMessage());
                                }
                            }
                            detailActivity.putExtra("id", temp.getObjectId());
                            detailActivity.putExtra("authors", authors);
                            detailActivity.putExtra("thumbnail", temp.getString("thumbnail"));
                            detailActivity.putExtra("isbn", temp.getString("isbn"));
                            detailActivity.putExtra("publisher", temp.getString("publisher"));
                            detailActivity.putExtra("publishedDate", temp.getString("publishedDate"));
                            detailActivity.putExtra("pageCount", temp.getInt("pageCount"));
                            startActivity(detailActivity);
                        }
                    });
                } else {
                    Log.d("Cataloguer", "ParseException: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.catalogue, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_sortAuthor) {
            catalogueItemAdapter.sortData(1);
            catalogueItemAdapter.notifyDataSetChanged();
        } else if (id == R.id.menu_sortTitle) {
            catalogueItemAdapter.sortData(0);
            catalogueItemAdapter.notifyDataSetChanged();
        }
        return super.onOptionsItemSelected(item);
    }
}
package com.rdmills.Cataloguer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
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
        query.whereContains("catalogue_id", catalogueId);
        query.whereEqualTo("active", true);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if(e == null) {
                    ListView listView = (ListView) findViewById(R.id.lv_catalogueItems);
                    catalogueItemAdapter = new CatalogueItemAdapter(list);
                    listView.setEmptyView(findViewById(R.id.tv_emptyList));
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
                                    //Do nothing
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
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == 0) {
            if(resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                Log.d("Cataloguer", "Contents: " + contents);
                DataFetcher dataFetcher = new DataFetcher(this);
                dataFetcher.execute(contents + "", catalogueId);
            } else if (resultCode == RESULT_CANCELED) {
                Log.d("Cataloguer", "RESULT_CANCELED");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.catalogue, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_scan) {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.setPackage("com.google.zxing.client.android");
            intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
            startActivityForResult(intent, 0);
        } else if (id == R.id.menu_sortAuthor) {
            catalogueItemAdapter.sortData(1);
            catalogueItemAdapter.notifyDataSetChanged();
        } else if (id == R.id.menu_sortTitle) {
            catalogueItemAdapter.sortData(0);
            catalogueItemAdapter.notifyDataSetChanged();
        }
        return super.onOptionsItemSelected(item);
    }

    public AlertDialog confirmDialog(String cIdIn, Book bookIn) {
        TextView info = new TextView(this);
        info.setPadding(5, 5, 5, 5);
        final Book book = bookIn;
        final String cId = cIdIn;
        info.setText(Html.fromHtml("Please confirm that <i>" + book.getTitle() + "</i> was scanned and should be added to the current catalogue"));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Scan");
        builder.setView(info);
        builder.setCancelable(false);
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ParseObject newBook = new ParseObject("Book");
                newBook.put("catalogue_id", cId);
                newBook.put("title", book.getTitle());
                newBook.put("subtitle", book.getSubtitle());
                newBook.put("authors", book.getAuthors());
                newBook.put("thumbnail", book.getThumbnail());
                newBook.put("isbn", book.getIsbn());
                newBook.put("publisher", book.getPublisher());
                newBook.put("publishedDate", book.getPublishedDate());
                newBook.put("pageCount", book.getPageCount());
                newBook.put("active", true);
                newBook.saveInBackground(new SaveCallback() {
                    public void done(ParseException e) {
                        if (e == null) {
                            Log.d("Cataloguer", "Great Success");
                            fillList();
                        } else {
                            Log.d("Cataloguer", "Error: " + e);
                        }
                    }
                });
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        builder.show();

        return builder.create();
    }
}
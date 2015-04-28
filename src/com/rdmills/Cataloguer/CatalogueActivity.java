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
import android.view.ViewGroup;
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
    private DataFetcher dataFetcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalogue);

        dataFetcher = new DataFetcher(this);

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

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == 0) {
            if(resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                Log.d("Cataloguer", "Contents: " + contents);
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

    public AlertDialog confirmDialog(String cIdIn, Book[] booksIn) {
        TextView info = new TextView(this);
        RelativeLayout rl = new RelativeLayout(this);
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 300);

        info.setPadding(5, 5, 5, 5);
        final Book[] books = booksIn;
        final String cId = cIdIn;

        info.setText(Html.fromHtml("Please select the scanned book from the list below or confirm that the book is missing."));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Scan");
        builder.setCancelable(false);

        final RadioGroup radioButtons = new RadioGroup(this);

        for(Book book : books) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(book.getTitle());
            radioButtons.addView(radioButton);
        }

        ScrollView scrollView = new ScrollView(this);

        RadioButton radioButton = new RadioButton(this);
        radioButton.setText("The book isn't there!");
        radioButtons.addView(radioButton);

        rl.addView(info, params1);
        info.setId(-1);
        scrollView.addView(radioButtons);
        params2.addRule(RelativeLayout.BELOW, info.getId());
        rl.addView(scrollView, params2);

        builder.setView(rl);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                int radioId = radioButtons.getCheckedRadioButtonId()-1;
                Book book = books[radioId];
                final String title = book.getTitle();
                ParseObject newBook = new ParseObject("Book");
                newBook.put("catalogue_id", cId);
                newBook.put("amazon_id", book.getAmazonId());
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
                            Log.d("Cataloguer", title + " successfully added");
                            fillList();
                        } else {
                            Log.d("Cataloguer", "Error: " + e);
                        }
                    }
                });
                dialog.dismiss();
            }
        });

        builder.show();

        return builder.create();
    }
}
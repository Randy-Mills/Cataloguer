package com.rdmills.Cataloguer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.parse.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MyActivity extends Activity {

    private Intent catalogueActivity;
    private SharedPreferences settings;
    private ListView cataloguesView;
    private Button scanBookButton;
    private Button textBookButton;
    private Button addCatalogueButton;

    private DataFetcher dataFetcher;

    private final int SCAN_CODE = 0;

    private ConnectivityManager connectivityManager;
    private NetworkInfo networkInfo;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Initialize Parse
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "GcExsXYqD8fWlkRbmw6Zv0st5beMr2ZekQOwDUsY", "QBzN8a0feWx629YGMz7cfG2QU3wHla6Wj23SDpTR");

        catalogueActivity = new Intent(this, CatalogueActivity.class);

        dataFetcher = new DataFetcher(this);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        settings = getSharedPreferences("prefs", 0);
        if(!settings.getBoolean("loggedIn", false)) {
            Intent logInIntent = new Intent(this, LoginActivity.class);
            startActivity(logInIntent);
        } else {
            cataloguesView = (ListView) findViewById(R.id.lv_catalogues);
            cataloguesView.setEmptyView(findViewById(R.id.rl_empty));

            addCatalogueButton = (Button) findViewById(R.id.btn_add_catalogue);
            addCatalogueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createDialog();
                }
            });

            scanBookButton = (Button) findViewById(R.id.btn_scan_search);
            scanBookButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                    intent.setPackage("com.google.zxing.client.android");
                    intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
                    startActivityForResult(intent, SCAN_CODE);
                }
            });

            textBookButton = (Button) findViewById(R.id.btn_text_search);
            textBookButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Do things
                }
            });

            fillList();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == SCAN_CODE) {
            if(resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");

                JSONArray localCollection = fetchLocalCollection();
                for(int i=0;i<localCollection.length();i++) {
                    try {
                        JSONObject temp = localCollection.getJSONObject(i);
                        if (temp.getString("isbn").contains(contents) || temp.getString("scan_isbn").equals(contents)) {
                            confirmLocal(temp.getString("scan_isbn"), temp.getString("title"), temp.getInt("quantity"));
                            return;
                        }
                    } catch (JSONException e) {
                        Log.d("Cataloguer", "JSONException: " + e.getMessage());
                    }
                }

                if(networkInfo.isConnected()) {
                    dataFetcher.execute(contents);
                }

            } else if (resultCode == RESULT_CANCELED) {
                Log.d("Cataloguer", "RESULT_CANCELED");
            }
        }
    }

    /**
     * Called when the application is closed.
     */
    @Override
    public void onStop() {
        super.onStop();

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if(networkInfo.isConnected()) {
            buildLocalCollection();
        }
    }

    private void buildLocalCollection() {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Catalogue");
        query.whereEqualTo("user_id", settings.getString("id", ""))
                .findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> list, ParseException e) {
                        if (e == null) {
                            ParseQuery<ParseObject> innerQuery = new ParseQuery<ParseObject>("Book");
                            List<String> catalogueIds = new ArrayList<String>();
                            for(ParseObject po : list) {
                                catalogueIds.add(po.getObjectId());
                            }
                            innerQuery.whereContainedIn("catalogue_id",catalogueIds)
                                    .findInBackground(new FindCallback<ParseObject>() {
                                        @Override
                                        public void done(List<ParseObject> list, ParseException e) {
                                            JSONArray jsonArray = new JSONArray();
                                            for (ParseObject book : list) {
                                                try {
                                                    JSONObject jsonObject = new JSONObject();
                                                    jsonObject.put("isbn", book.getString("isbn"));
                                                    jsonObject.put("scan_isbn", book.getString("scan_isbn"));
                                                    jsonObject.put("title", book.getString("title"));
                                                    jsonObject.put("quantity", book.getInt("quantity"));
                                                    jsonArray.put(jsonObject);
                                                } catch (JSONException jsonException) {
                                                    jsonException.printStackTrace();
                                                }
                                            }
                                            SharedPreferences lib = getSharedPreferences("libs", MODE_PRIVATE);
                                            lib.edit().clear();
                                            lib.edit().putString("lib", jsonArray.toString()).commit();
                                            Log.d("Cataloguer", "Wrote to local collection");
                                        }
                                    });
                        } else {
                            Log.d("Cataloguer", "ParseException: " + e.getMessage());
                        }
                    }
                });
    }

    private JSONArray fetchLocalCollection() {
        SharedPreferences lib = getSharedPreferences("libs", 0);
        try {
            return new JSONArray(lib.getString("lib", "[]"));
        } catch (JSONException e) {
            Log.d("Cataloguer", "ParseException: " + e.getMessage());
        }

        Log.d("Cataloguer", "GeneralError: Local collection is null");
        return null;
    }

    private void fillList() {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Catalogue");
        query.whereEqualTo("user_id", settings.getString("id", ""))
             .findInBackground(new FindCallback<ParseObject>() {
                 @Override
                 public void done(List<ParseObject> list, ParseException e) {
                     if (e == null) {
                         final CatalogueAdapter catalogueAdapter = new CatalogueAdapter(list);
                         cataloguesView.setAdapter(catalogueAdapter);

                         cataloguesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                             @Override
                             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                 ParseObject temp = catalogueAdapter.getItem(position);
                                 Log.d("Cataloguer", temp.getString("title") + ": " + temp.getObjectId());
                                 catalogueActivity.putExtra("cId", temp.getObjectId());
                                 startActivity(catalogueActivity);
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
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_addCatalogue) {
            createDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    //Dialogs
    public AlertDialog createDialog() {
        final EditText name = new EditText(this);
        name.setText("New Catalogue");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Catalogue Name");
        builder.setView(name);
        builder.setCancelable(true);
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParseObject newCatalogue = new ParseObject("Catalogue");
                newCatalogue.put("user_id", settings.getString("id", ""));
                newCatalogue.put("title", name.getText().toString());
                newCatalogue.put("type", "Books");
                newCatalogue.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e == null) {
                            Log.d("Cataloguer", "New Catalogue added successfully");
                            fillList();
                        } else {
                            Log.d("Cataloguer", "Error: " + e.getMessage());
                        }
                    }
                });
                dialog.dismiss();
            }
        });

        builder.show();

        return builder.create();
    }

    public AlertDialog confirmDialog(Book[] booksIn) {
        TextView info = new TextView(this);
        RelativeLayout rl = new RelativeLayout(this);
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 300);

        info.setId(View.generateViewId());
        final Book[] books = booksIn;

        info.setText(Html.fromHtml("Select the correct book."));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Book Found");
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
        scrollView.addView(radioButtons);
        params2.addRule(RelativeLayout.BELOW, info.getId());
        scrollView.setLayoutParams(params2);
        rl.addView(scrollView);

        builder.setView(rl);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                int radioId = radioButtons.getCheckedRadioButtonId()-2;
                Book book = books[radioId];
                final String title = book.getTitle();
                ParseObject newBook = new ParseObject("Book");
                newBook.put("catalogue_id", "");
                newBook.put("amazon_id", book.getAmazonId());
                newBook.put("title", book.getTitle());
                newBook.put("subtitle", book.getSubtitle());
                newBook.put("authors", book.getAuthors());
                newBook.put("thumbnail", book.getThumbnail());
                newBook.put("isbn", book.getIsbn());
                newBook.put("scan_isbn", book.getScanIsbn());
                newBook.put("publisher", book.getPublisher());
                newBook.put("publishedDate", book.getPublishedDate());
                newBook.put("pageCount", book.getPageCount());
                newBook.put("active", true);
                newBook.put("quantity", 1);
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

    public AlertDialog confirmLocal(final String scanIsbn, String title, int quantity) {
        final TextView info = new TextView(this);
        info.setPadding(5,5,5,5);
        if(quantity == 1) {
            info.setText("You have 1 copy of " + title + ". To add another copy to your collection click Add.");
        } else {
            info.setText("You have " + quantity + " copies of " + title + ". To add another copy to your collection click Add.");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Found Locally");
        builder.setView(info);

        builder.setCancelable(true);
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Book");
                query.whereEqualTo("scan_isbn", scanIsbn);
                query.getFirstInBackground(new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject parseObject, ParseException e) {
                        Log.d("Cataloguer", "Updating: " + parseObject.getObjectId());
                        int temp = parseObject.getInt("quantity");
                        parseObject.put("quantity", temp+1);
                        parseObject.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if(e == null) {
                                    Log.d("Cataloguer", "Collection updated");
                                } else {
                                    Log.d("Cataloguer", "ParseException: " + e.getMessage());
                                }
                            }
                        });
                    }
                });
                dialog.dismiss();
            }
        });

        builder.show();

        return builder.create();
    }
}

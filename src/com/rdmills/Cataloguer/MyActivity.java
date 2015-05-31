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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.parse.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MyActivity extends Activity {

    private AlertDialog.Builder alertDialogBuilder;
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

        alertDialogBuilder = new AlertDialog.Builder(this);

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
                    createCatalogue();
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
                            createConfirmLocal(temp.getString("scan_isbn"), temp.getString("title"), temp.getInt("quantity"));
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
            createCatalogue();
        }
        return super.onOptionsItemSelected(item);
    }

    //Dialogs

    /**
     * Builds a New Catalogue Dialog that prompts users for information to create a Catalogue object on Parse.
     *
     * @return Dialog box with users directions to create new Catalogue object.
     */
    public AlertDialog createCatalogue() {
        final EditText name = new EditText(this);
        name.setText("New Catalogue");
        alertDialogBuilder.setTitle("Catalogue Name");
        alertDialogBuilder.setView(name);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParseObject newCatalogue = new ParseObject("Catalogue");
                newCatalogue.put("user_id", settings.getString("id", ""));
                newCatalogue.put("title", name.getText().toString());
                newCatalogue.put("type", "Books"); //Default to books
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

        alertDialogBuilder.show();

        return alertDialogBuilder.create();
    }

    /**
     * Builds a Selector Dialog that prompts users to select the proper book from the list of isbn sharing options.
     * Once an option has been selected users can continue on to add the book to a catalogue.
     *
     * @return Dialog box with users directions to create new Catalogue object.
     */
    public AlertDialog createBookSelector(Book[] booksIn) {
        TextView info = new TextView(this);
        RelativeLayout rl = new RelativeLayout(this);
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 300);

        info.setId(View.generateViewId());
        final Book[] books = booksIn;

        alertDialogBuilder.setTitle("Book Found");
        alertDialogBuilder.setCancelable(false);

        info.setText(Html.fromHtml("Select the correct book."));

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

        alertDialogBuilder.setView(rl);
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        alertDialogBuilder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                int radioId = radioButtons.getCheckedRadioButtonId()-2;
                Book book = books[radioId];

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

                dialog.dismiss();

                if(cataloguesView.getCount() == 1) {
                    ParseObject catalogue = (ParseObject) cataloguesView.getAdapter().getItem(0);
                    newBook.put("catalogue_id", catalogue.getObjectId());
                    saveBook(newBook);
                } else {
                    createCatalogueSelector(newBook);
                }
            }
        });

        alertDialogBuilder.show();

        return alertDialogBuilder.create();
    }

    private AlertDialog createCatalogueSelector(ParseObject book) {
        AlertDialog.Builder alertDialogBuilder2 = new AlertDialog.Builder(this);
        alertDialogBuilder2.setTitle("Select Catalogue");

        final ParseObject newBook = book;
        final List<ParseObject> catalogues = ((CatalogueAdapter)cataloguesView.getAdapter()).getData();

        TextView info = new TextView(this);
        RelativeLayout rl = new RelativeLayout(this);
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 300);

        info.setId(View.generateViewId());

        info.setText(Html.fromHtml("Select the catalogue you want to add the book to."));

        final RadioGroup radioButtons = new RadioGroup(this);

        for(ParseObject catalogue : catalogues) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(catalogue.getString("title"));
            radioButtons.addView(radioButton);
        }

        ScrollView scrollView = new ScrollView(this);

        rl.addView(info, params1);
        scrollView.addView(radioButtons);
        params2.addRule(RelativeLayout.BELOW, info.getId());
        scrollView.setLayoutParams(params2);
        rl.addView(scrollView);

        alertDialogBuilder2.setView(rl);
        alertDialogBuilder2.setCancelable(true);
        alertDialogBuilder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        alertDialogBuilder2.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int radioId = radioButtons.getCheckedRadioButtonId()-14;
                System.out.println(radioId);
                System.out.println(catalogues.get(radioId).getObjectId() + " " + catalogues.get(radioId).getString("title"));
                newBook.put("catalogue_id", catalogues.get(radioId).getObjectId());
                saveBook(newBook);
            }
        });

        alertDialogBuilder2.show();

        return alertDialogBuilder2.create();
    }

    public AlertDialog createConfirmLocal(final String scanIsbn, String title, int quantity) {
        final TextView info = new TextView(this);
        info.setPadding(5,5,5,5);
        if(quantity == 1) {
            info.setText("You have 1 copy of " + title + ". To add another copy to your collection click Add.");
        } else {
            info.setText("You have " + quantity + " copies of " + title + ". To add another copy to your collection click Add.");
        }
        alertDialogBuilder.setTitle("Found Locally");
        alertDialogBuilder.setView(info);

        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
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

        alertDialogBuilder.show();

        return alertDialogBuilder.create();
    }

    private void saveBook(ParseObject book) {
        final String title = book.getString("title");
        book.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("Cataloguer", title + " successfully added");
                    fillList();
                } else {
                    Log.d("Cataloguer", "Error: " + e);
                }
            }
        });
    }
}

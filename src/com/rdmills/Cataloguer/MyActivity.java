package com.rdmills.Cataloguer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import com.parse.*;

import java.util.List;

public class MyActivity extends Activity {

    private Intent catalogueActivity;
    private SharedPreferences settings;
    private ListView cataloguesView;

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

        settings = getSharedPreferences("prefs", 0);
        if(!settings.getBoolean("loggedIn", false)) {
            Intent logInIntent = new Intent(this, LoginActivity.class);
            startActivity(logInIntent);
        }

        cataloguesView = (ListView) findViewById(R.id.lv_catalogues);
        cataloguesView.setEmptyView(findViewById(R.id.rl_empty));

        fillList();
    }

    private void fillList() {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Catalogue");
        query.whereEqualTo("user_id", settings.getString("id", ""));
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if(e == null) {
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
                    Log.d("Cataloguer",  "ParseException: " + e.getMessage());
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
}

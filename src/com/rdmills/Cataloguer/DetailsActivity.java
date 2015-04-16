package com.rdmills.Cataloguer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import org.json.JSONArray;

import java.io.InputStream;

/**
 * Created by Randy on 16/04/2015.
 */
public class DetailsActivity extends Activity {

    private ViewSwitcher viewSwitcher;

    private ImageView detailImage;
    private TextView titleField;
    private TextView subtitleField;
    private TextView authorsField;
    private TextView publisherField;
    private TextView publishDateField;
    private TextView pageCountField;

    private ImageView detailImage2;
    private EditText titleEdit;
    private EditText subtitleEdit;
    private EditText authorsEdit;
    private EditText publisherEdit;
    private EditText publishDateEdit;
    private EditText pageCountEdit;

    private String title;
    private String subtitle;
    private String[] authors;
    private String author;
    private String thumbnail;
    private String isbn;
    private String publisher;
    private String publishedDate;
    private int pageCount;

    private String bookId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        viewSwitcher = (ViewSwitcher) findViewById(R.id.vs_detail);

        detailImage = (ImageView) findViewById(R.id.iv_detailImage);
        titleField = (TextView) findViewById(R.id.tv_detailTitle);
        subtitleField = (TextView) findViewById(R.id.tv_detailSubtitle);
        authorsField = (TextView) findViewById(R.id.tv_detailAuthors);
        publisherField = (TextView) findViewById(R.id.tv_detailPublisher);
        publishDateField = (TextView) findViewById(R.id.tv_detailPublishDate);
        pageCountField = (TextView) findViewById(R.id.tv_detailPageCount);

        detailImage2 = (ImageView) findViewById(R.id.iv_detailImageSwitch);
        titleEdit = (EditText) findViewById(R.id.et_detailTitle);
        subtitleEdit = (EditText) findViewById(R.id.et_detailSubtitle);
        authorsEdit = (EditText) findViewById(R.id.et_detailAuthors);
        publisherEdit = (EditText) findViewById(R.id.et_detailPublisher);
        publishDateEdit = (EditText) findViewById(R.id.et_detailPublishDate);
        pageCountEdit = (EditText) findViewById(R.id.et_detailPageCount);

        title = subtitle = thumbnail = isbn = publisher = publishedDate = "";
        pageCount = 0;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            bookId = extras.getString("id");
            title = extras.getString("title");
            subtitle = extras.getString("subtitle");
            authors = (String[]) extras.get("authors");
            thumbnail = extras.getString("thumbnail");
            isbn = extras.getString("isbn");
            publisher = extras.getString("publisher");
            publishedDate = extras.getString("publishedDate");
            pageCount = extras.getInt("pageCount");

            author = "";

            titleField.setText(title);
            titleEdit.setText(title);

            subtitleField.setText(subtitle);
            subtitleEdit.setText(subtitle);

            for(String s : authors) {
                author = author + ", " + s;
            }

            if(author.length() > 0)
                author = author.substring(2);

            authorsField.setText(author);
            authorsEdit.setText(author);

            publisherField.setText(publisher);
            publisherEdit.setText(publisher);

            publishDateField.setText(publishedDate);
            publishDateEdit.setText(publishedDate);

            pageCountField.setText(pageCount+"");
            pageCountEdit.setText(pageCount+"");

            if(!thumbnail.equals("")) {
                thumbnail = thumbnail.replace("zoom=5", "zoom=1");
                new DownloadImageTask(detailImage).execute(thumbnail);
                //Couldn't come up with a solid work around for this when I switch to edit mode...so I call it twice like a scrub
                new DownloadImageTask(detailImage2).execute(thumbnail);
            } else {
                detailImage.setImageResource(R.drawable.no_image);
                detailImage.setImageResource(R.drawable.no_image);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_edit) {
            if(item.getTitle().equals("Edit")) {
                item.setTitle("Save");
            } else {
                saveChanges();
                item.setTitle("Edit");
            }

            viewSwitcher.showNext();

            return true;
        } else if (id == R.id.menu_delete) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
            query.getInBackground(bookId, new GetCallback<ParseObject>() {
                public void done(ParseObject book, ParseException e) {
                    if (e == null) {
                        book.put("active", false);
                        book.saveInBackground();
                        finish();
                    }
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveChanges() {
        title = titleEdit.getText().toString();
        subtitle = subtitleEdit.getText().toString();
        author = authorsEdit.getText().toString();
        publisher = publisherEdit.getText().toString();
        publishedDate = publishDateEdit.getText().toString();
        pageCount = Integer.parseInt(pageCountEdit.getText().toString());

        titleField.setText(title);
        subtitleField.setText(subtitle);
        authorsField.setText(author);
        publisherField.setText(publisher);
        publishDateField.setText(publishedDate);
        pageCountField.setText(pageCount+"");

        Log.d("Cataloguer", "saveChanges to: " + bookId);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
        query.getInBackground(bookId, new GetCallback<ParseObject>() {
            public void done(ParseObject book, ParseException e) {
                if (e == null) {
                    Log.d("Cataloguer","Object Found");

                    book.put("title", title);
                    book.put("subtitle", subtitle);
                    author = author.replaceAll(",  ", ",");
                    String[] tempAuthorArray = author.split(",");
                    JSONArray authorArray = new JSONArray();
                    for(String s : tempAuthorArray) {
                        authorArray.put(s);
                    }
                    book.put("authors", authorArray);
                    book.put("publisher", publisher);
                    book.put("publishedDate", publishedDate);
                    book.put("pageCount", pageCount);
                    book.saveInBackground();
                    Log.d("Cataloguer","Saving!");
                } else {
                    Log.d("Cataloger","e: " + e.getMessage());
                }
            }
        });
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urlDisplay).openStream();
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
}
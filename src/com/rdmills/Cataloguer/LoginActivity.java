package com.rdmills.Cataloguer;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.parse.*;

/**
 * Created by Randy on 16/04/2015.
 */
public class LoginActivity extends Activity {

    private Button loginBtn;
    private Button registerBtn;
    private EditText usernameField;
    private EditText passwordField;
    private TextView feedbackField;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginBtn = (Button) findViewById(R.id.btn_login);
        registerBtn = (Button) findViewById(R.id.btn_register);
        usernameField = (EditText) findViewById(R.id.et_username);
        passwordField = (EditText) findViewById(R.id.et_password);
        feedbackField = (TextView) findViewById(R.id.tv_feedback);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();

                password = encodePassword(password);

                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("User");
                query.whereEqualTo("active", true);
                query.whereEqualTo("userName", username);
                query.whereEqualTo("password", password);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> list, ParseException e) {
                        if(e == null) {
                            ParseObject temp;
                            if(list.size() > 0) {
                                temp = list.get(0);
                                if(temp.isDataAvailable()) {
                                    goHome(temp.getObjectId());
                                }
                            }

                            feedbackField.setText("User not found. Please check the details you provided");
                        } else {
                            feedbackField.setText("An error occurred. Please try again.");
                        }
                    }
                });
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmPassword();
            }
        });
    }

    private void goHome(String id) {
        Log.d("Cataloguer", "Fetch id: " + id);
        SharedPreferences settings = getSharedPreferences("prefs", 0);
        settings.edit().putBoolean("loggedIn", true).commit();
        settings.edit().putString("id", id).commit();
        Intent homeIntent = new Intent(this, MyActivity.class);
        startActivity(homeIntent);
    }

    private String encodePassword(String passwordIn) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(passwordIn.getBytes("UTF-8"));

            byte[] byteData = md.digest();

            BigInteger bigInt = new BigInteger(1, byteData);
            String pass = bigInt.toString(16);
            while(pass.length() < 64)
                pass = "0" + pass;
            return pass;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return ""; //Password encoding failed...but it shouldn't...ever
    }

    //Hitting back on the login screen should 'close' the app, not return to the main screen pre-routing
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public AlertDialog confirmPassword() {
        final EditText repeat = new EditText(this);
        repeat.setHint("Repeat password");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Password");
        builder.setView(repeat);
        builder.setCancelable(true);
        builder.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(repeat.getText().toString().equals(passwordField.getText().toString())) {
                    ParseObject newUser = new ParseObject("User");
                    newUser.put("userName", usernameField.getText().toString());
                    newUser.put("password", encodePassword(passwordField.getText().toString()));
                    newUser.put("active", true);
                    newUser.saveInBackground(new SaveCallback() {
                        public void done(ParseException e) {
                            if (e == null) {
                                Log.d("Cataloguer", "New User added successfully");
                                feedbackField.setText("Registration Complete");
                            } else {
                                Log.d("Cataloguer", "Error: " + e);
                            }
                        }
                    });
                    feedbackField.setText("Registration in progress");
                } else {
                    feedbackField.setText("Password did not match");
                    dialog.dismiss();
                }
            }
        });

        builder.show();

        return builder.create();
    }
}
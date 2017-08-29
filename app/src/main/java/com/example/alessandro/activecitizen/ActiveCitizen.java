package com.example.alessandro.activecitizen;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

import static android.R.attr.colorBackground;
import static android.R.string.no;

public class ActiveCitizen extends AppCompatActivity {

    protected RequestQueue queue;               // The queue of requests to be sent to the server

    // Account informations
    protected int userId;                       // ID uniquely identifying one account
    protected String username;                  // Account username
    protected String password;                  // Account password
    protected SharedPreferences preferences;    // Preferences permanently storing account data
    protected boolean fromConfigurationChange;  // Stores whether the activity was created after a configuration change
    static int logged;                          // Stores whether the user is logged

    protected AlertDialog accountDialog;        // Dialog allowing the user to login/register
    protected String existingUsernameRestored;  // Copy of the accountDialog username field in login form
    protected String existingPasswordRestored;  // Copy of the accountDialog password field in login form
    protected String newUsernameRestored;       // Copy of the accountDialog username field in register form
    protected String newPasswordRestored;       // Copy of the accountDialog password field in register form

    // Views of the account dialog, whose references are used to save and restore their values
    protected EditText editText_existingUsername;
    protected EditText editText_existingPassword;
    protected EditText editText_newUsername;
    protected EditText editText_newPassword;

    protected TextView textView_activeCitizen_hello;            // TextView greeting users
    protected ProgressBar progressBar_activeCitizen_loading;    // Loading ProgressBar

    protected RelativeLayout relativeLayout;                    // Layout. Used to change background_color

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initializations
        setContentView(R.layout.activity_active_citizen);
        queue = Volley.newRequestQueue(this);
        preferences = getSharedPreferences("account_information", MODE_PRIVATE);
        textView_activeCitizen_hello = (TextView) findViewById(R.id.textView_activeCitizen_hello);
        progressBar_activeCitizen_loading = (ProgressBar) findViewById(R.id.progressBar_activeCitizen_loading);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout_active_citizen);
        logged = 0;

        // Retrieves account informations from non-volatile memory
        username = preferences.getString("username", "");
        password = preferences.getString("password", "");

        // Checks if this is an application start or a device configuration change
        if(savedInstanceState != null){
            fromConfigurationChange = true;
        }
    }

    @Override
    protected void onResume() {
        System.out.println("[DEBUG] onResume()");
        super.onResume();

        if(fromConfigurationChange){
            // A rotation has occurred
            if(logged == 1){
                // A rotation has occurred; the user is already logged; nothing has to be done
                //System.out.println("[DEBUG] onResume after rotation. I dismiss the bar since logged=" + logged);
                //progressBar_activeCitizen_loading.setVisibility(View.GONE);
                //relativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.default_background));
                //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            } else {
                // A rotation has occurred; the user is not logged yet; show loading dialog
                //System.out.println("[DEBUG] onResume after rotation. I show the bar since logged=" + logged);
                //progressBar_activeCitizen_loading.setVisibility(View.VISIBLE);
                //relativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.uninteractive_screen));
                //getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                  //      WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        } else {
            // The application has been started from scratch
            if(logged == 0) {
                // Just to avoid showing progressBar in case we go back to this activity from another one
                if (username.equals("")) {
                    // The application has been just started, and no account informations exist on disk
                    showAccountDialog(null);
                } else {
                    // The application has just been started, and account informations exist on disk
                    System.out.println("[DEBUG] application just started and stored data existing, I'll show the bar");
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    relativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.uninteractive_screen));
                    progressBar_activeCitizen_loading.setVisibility(View.VISIBLE);
                    login(username, password);
                }
            }
        }
        System.out.println("[DEBUG] onResume(), user e pass e id sono " + username + ", " + password + ", " + userId);
    }

    @Override
    protected void onPause(){
        super.onPause();
        System.out.println("[DEBUG] onPause()");
    }

    @Override
    protected void onStop(){
        super.onStop();
        System.out.println("[DEBUG] onStop()");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        System.out.println("[DEBUG] onDestroy()");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        System.out.println("[DEBUG] onSaveInstanceState, logged vale " + logged);
        outState.putInt("logged", logged);
        outState.putInt("userid", userId);
        outState.putString("username", username);
        outState.putString("password", password);
        if((editText_newUsername != null) && (accountDialog != null && accountDialog.isShowing())) {
            outState.putString("new_username", editText_newUsername.getText().toString());
            outState.putString("new_password", editText_newPassword.getText().toString());
            outState.putString("existing_username", editText_existingUsername.getText().toString());
            outState.putString("existing_password", editText_existingPassword.getText().toString());
        } else {
            //System.out.println("[DEBUG] newUsername is null, thus dialog field are not saved");
        }
        super.onSaveInstanceState(outState);
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            logged = savedInstanceState.getInt("logged", 100);
            System.out.println("[DEBUG] onRestoreInstanceState(), logged vale " + logged);
            userId = savedInstanceState.getInt("userid");
            username = savedInstanceState.getString("username");
            password = savedInstanceState.getString("password");
            System.out.println("[DEBUG] onRestoreInstanceState(), user e pass e id sono " + username + ", " + password + ", " + userId);
            if(!username.equals("")){
                textView_activeCitizen_hello.setText("Welcome, " + username);
            }
            existingUsernameRestored = savedInstanceState.getString("existing_username");
            existingPasswordRestored = savedInstanceState.getString("existing_password");
            newUsernameRestored = savedInstanceState.getString("new_username");
            newPasswordRestored = savedInstanceState.getString("new_password");
            if(editText_newUsername != null) {
                System.out.println("[DEBUG] editText_newUsername is not null!");
                editText_existingUsername.setText(existingUsernameRestored);
                editText_existingPassword.setText(existingPasswordRestored);
                editText_newUsername.setText(newUsernameRestored);
                editText_newPassword.setText(newPasswordRestored);
            }
        }
    }

    public void showAccountDialog(View v) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View inflater = layoutInflater.inflate(R.layout.dialog_account, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(inflater);
        //builder.setCancelable(false);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        accountDialog = builder.show();

        Button loginButton = (Button) inflater.findViewById(R.id.button_dialogAccount_login);
        Button registerButton = (Button) inflater.findViewById(R.id.button_dialogAccount_register);
        editText_existingUsername = (EditText) inflater.findViewById(R.id.editText_dialogAccount_existingUsername);
        editText_existingPassword = (EditText) inflater.findViewById(R.id.editText_dialogAccount_existingPassword);
        editText_newUsername = (EditText) inflater.findViewById(R.id.editText_dialogAccount_newUsername);
        editText_newPassword = (EditText) inflater.findViewById(R.id.editText_dialogAccount_newPassword);
        editText_existingUsername.setText(existingUsernameRestored);
        editText_existingPassword.setText(existingPasswordRestored);
        editText_newUsername.setText(newUsernameRestored);
        editText_newPassword.setText(newPasswordRestored);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editText_newUsername.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(), "You must insert an username", Toast.LENGTH_LONG).show();
                } else {
                    System.out.println("[DEBUG] I'm going to execute register method");
                    register(editText_newUsername.getText().toString(),
                            editText_newPassword.getText().toString());
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editText_existingUsername.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(), "You must insert an username", Toast.LENGTH_LONG).show();
                } else {
                    login(editText_existingUsername.getText().toString(),
                            editText_existingPassword.getText().toString());
                }
            }
        });
    }

    private int login(final String existingUsername, final String existingPassword) {
        String url = "http://www.activecitizen.altervista.org/login/";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println("[DEBUG] a response to the login operation arrived: " + response);
                        // response
                        Log.d("[DEBUG] Response", response);
                        if(response.equals("0")){
                            Toast.makeText(getApplicationContext(), "Invalid username or password ", Toast.LENGTH_LONG).show();
                            //if(loadingDialog != null && loadingDialog.isShowing()){
                          //      loadingDialog.dismiss();
                         //   }
                            if(accountDialog != null && !(accountDialog.isShowing())) {
                                showAccountDialog(null);
                            }
                        } else {
                            userId = Integer.parseInt(response);
                            username = existingUsername;
                            password = existingPassword;
                            logged = 1;
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("userid", userId);
                            editor.putString("username", username);
                            editor.putString("password", password);
                            editor.commit();
                            System.out.println("[DEBUG] login successful, user e pass e id sono " + username + ", " + password + ", " + userId + ". logged is " + logged);
                            System.out.println("[DEBUG] login(), sto per mandare in \"gone\" il dialog. " +
                                    "Il riferimento alla ProgressBar vale " + progressBar_activeCitizen_loading);
                            textView_activeCitizen_hello.setText("Welcome, " + username);
                            progressBar_activeCitizen_loading.setVisibility(View.GONE);
                            //relativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.default_background));
                            relativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.background_light));
                            //android:colorBackground;


                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            if(accountDialog != null) {
                                System.out.println("[DEBUG] in login, I'm going to dismiss accountDialog");
                                accountDialog.dismiss();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //TODO: ricordarsi di gestire questo caso: devo rimuovere il dialogo
                        // error
                        String message = "Server unreachable. Check your internet connection";
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                      //  if(loadingDialog != null && loadingDialog.isShowing()){
                     //      loadingDialog.dismiss();
                     //   }
                        finish();
                        Log.d("[DEBUG] Error.Response", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("username", existingUsername);
                params.put("password", existingPassword);
                return params;
            }
        };
        queue.add(postRequest);
        return 0;
    }

    private int register(final String newUsername, final String newPassword){
        String url = "http://www.activecitizen.altervista.org/register/";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println("[DEBUG] a response to the login operation arrived");
                        // response
                        Log.d("[DEBUG] Response", response);
                        if(response.equals("0")){
                            Toast.makeText(getApplicationContext(), "Username already used, choose another", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Account created successfully", Toast.LENGTH_LONG).show();
                            userId = Integer.parseInt(response);
                            username = newUsername;
                            password = newPassword;
                            textView_activeCitizen_hello.setText("Welcome, " + username);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("userid", userId);
                            editor.putString("username", username);
                            editor.putString("password", password);
                            editor.commit();
                            System.out.println("[DEBUG] registration successful, user e pass e id sono " + username + ", " + password + ", " + userId);
                            if(accountDialog != null) {
                                System.out.println("[DEBUG] in registration, I'm going to dismiss accountDialog");
                                accountDialog.dismiss();
                            }
                            //dialog.cancel();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("[DEBUG] Error.Response", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("username", newUsername);
                params.put("password", newPassword);
                return params;
            }
        };
        queue.add(postRequest);
        return 0;
    }

    public void launchReportAnIssue(View v) {
        if(userId == 0){
            Toast.makeText(getApplicationContext(), "Account error", Toast.LENGTH_LONG).show();
            finish();
        }
        System.out.println("[DEBUG] Sending an intent to ReportAnIssue");
        Intent i = new Intent(this, ReportAnIssue.class);
        i.putExtra("user_id", userId);
        startActivity(i);
    }

    public void launchBrowseMap(View v) {
        if(userId == 0){
            Toast.makeText(getApplicationContext(), "Account error", Toast.LENGTH_LONG).show();
            finish();
        }
        System.out.println("[DEBUG] Sending an intent to BrowseMap");
        Intent i = new Intent(this, BrowseMap.class);
        i.putExtra("user_id", userId);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.activity_active_citizen_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                System.out.println("[DEBUG] logout pressed");
                userId = 0;
                username = "";
                password = "";
                textView_activeCitizen_hello.setText("");
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("userid", 0);
                editor.putString("username", "");
                editor.putString("password", "");
                editor.commit();
                showAccountDialog(null);
                logged = 0;
                return true;
            case R.id.settings:
                System.out.println("[DEBUG] settings pressed");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

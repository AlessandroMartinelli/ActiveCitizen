package com.example.alessandro.activecitizen;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import static android.R.id.message;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;
import static com.example.alessandro.activecitizen.R.id.editText_dialogAccount_existingUsername;
import static com.example.alessandro.activecitizen.R.id.username;

public class ActiveCitizen extends AppCompatActivity {

    private RequestQueue queue;
    private String url;

    private AlertDialog loginDialog;
    private View inflater;
    private ProgressDialog loginLoadingDialog;

    private int userId;
    private String username;
    private String password;
    private SharedPreferences preferences;

    private String existingUsernameRestored;
    private String existingPasswordRestored;
    private String newUsernameRestored;
    private String newPasswordRestored;

    private EditText editText_existingUsername;
    private EditText editText_existingPassword;
    private EditText editText_newUsername;
    private EditText editText_newPassword;
    private TextView textView_activeCitizen_hello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_citizen);
        
        queue = Volley.newRequestQueue(this);
        url = "http://www.activecitizen.altervista.org";
        preferences = getSharedPreferences("account_information", MODE_PRIVATE);
        textView_activeCitizen_hello = (TextView) findViewById(R.id.textView_activeCitizen_hello);

        userId = preferences.getInt("userid", 0);
        username = preferences.getString("username", "");
        password = preferences.getString("password", "");

        if((savedInstanceState == null)&&(!username.equals(""))) {
            // If the application has just been started, i.e.
            // this onCreate() is not caused by a screen rotation.
            // User was logged in the previous session.
            // The login operation has to be repeated
            // each time the application is started.
            System.out.println("[DEBUG] sto per eseguire login()");
            login(username, password);
            System.out.println("[DEBUG] sto per eseguire ProgressDialog.show()");
            loginLoadingDialog = ProgressDialog.show(this, "", "Please wait...", true);
        }
        System.out.println("[DEBUG] onCreate(), user e pass e id sono " + username + ", " + password + ", " + userId);
    }

    @Override
    protected void onResume() {
        System.out.println("[DEBUG] onResume()");
        super.onResume();
        if ((username.equals("")) && (loginDialog==null || !(loginDialog.isShowing()))) {
            // User has not logged yet
            System.out.println("[DEBUG] onResume(), I'm going to show accountDialog");
            showAccountDialog(null);
        }
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
        System.out.println("[DEBUG] onSaveInstanceState()");
        outState.putInt("userid", userId);
        outState.putString("username", username);
        outState.putString("password", password);
        if((editText_newUsername != null) && (loginDialog.isShowing())) {
            // andrebbe fatto solo se dialog is showing...
            System.out.println("[DEBUG] sto salvando " + editText_newUsername.getText().toString());
            outState.putString("new_username", editText_newUsername.getText().toString());
            outState.putString("new_password", editText_newPassword.getText().toString());
            outState.putString("existing_username", editText_existingUsername.getText().toString());
            outState.putString("existing_password", editText_existingPassword.getText().toString());
        } else {
            System.out.println("[DEBUG] newUsername is null, thus dialog field are not saved");
        }
        super.onSaveInstanceState(outState);
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState){
        System.out.println("[DEBUG] onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
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
        inflater = layoutInflater.inflate(R.layout.dialog_account, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(inflater);
        //builder.setCancelable(false);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        loginDialog = builder.show();

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
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println("[DEBUG] a response to the login operation arrived: " + response);
                        // response
                        Log.d("[DEBUG] Response", response);
                        if(response.equals("0")){
                            Toast.makeText(getApplicationContext(), "Invalid username or password ", Toast.LENGTH_LONG).show();
                            if(loginLoadingDialog != null && loginLoadingDialog.isShowing()){
                                loginLoadingDialog.dismiss();
                            }
                            if(loginDialog != null && !(loginDialog.isShowing())) {
                                showAccountDialog(null);
                            }
                        } else {
                            userId = Integer.parseInt(response);
                            username = existingUsername;
                            password = existingPassword;
                            textView_activeCitizen_hello.setText("Welcome, " + username);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("userid", userId);
                            editor.putString("username", username);
                            editor.putString("password", password);
                            editor.commit();
                            System.out.println("[DEBUG] login successful, user e pass e id sono " + username + ", " + password + ", " + userId);
                            if(loginLoadingDialog != null && loginLoadingDialog.isShowing()){
                                loginLoadingDialog.dismiss();
                            }
                            if(loginDialog != null) {
                                System.out.println("[DEBUG] in login, I'm going to dismiss loginDialog");
                                loginDialog.dismiss();
                            }
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
                params.put("action", "login");
                params.put("username", existingUsername);
                params.put("password", existingPassword);
                return params;
            }
        };
        queue.add(postRequest);
        return 0;
    }

    private int register(final String newUsername, final String newPassword){
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
                            if(loginDialog != null) {
                                System.out.println("[DEBUG] in registration, I'm going to dismiss loginDialog");
                                loginDialog.dismiss();
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
                params.put("action", "register");
                params.put("username", newUsername);
                params.put("password", newPassword);
                return params;
            }
        };
        queue.add(postRequest);
        return 0;
    }

    public void launchReportAnIssue(View v) {
        System.out.println("[DEBUG] Sending an intent to ReportAnIssue");
        Intent i = new Intent(this, ReportAnIssue.class);
        i.putExtra("user_id", userId);
        startActivity(i);
    }

    public void launchBrowseMap(View v) {
        System.out.println("[DEBUG] Sending an intent to BrowseMap");
        Intent i = new Intent(this, ManualCoordinates.class);
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
                return true;
            case R.id.settings:
                System.out.println("[DEBUG] settings pressed");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

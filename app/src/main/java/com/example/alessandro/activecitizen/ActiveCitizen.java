// TODO ci sono alcune situazioni strane, come browseMap crasha, nelle quali, una volta tornati
// ad activecitizen, qui il tasto "logout" non e' presente.

package com.example.alessandro.activecitizen;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

public class ActiveCitizen extends AppCompatActivity {

    protected static RequestQueue queue;               // The queue of requests to be sent to the server

    // Account informations
    protected static int userId;                       // ID uniquely identifying one account
    protected static String username;                  // Account username
    protected static String password;                  // Account password
    protected static SharedPreferences preferences;    // Preferences permanently storing account data
    protected boolean fromConfigurationChange;  // Stores whether the activity was created after a configuration change
    protected static int logged;                          // Stores whether the user is logged
    protected static int progressBarShown;
    protected static boolean confirmExit;

    protected static DialogFragment accountDialog;                  // Dialog allowing the user to login/register
    protected static TextView textView_activeCitizen_hello;         // TextView greeting users
    protected static ProgressBar progressBar_activeCitizen_loading; // Loading ProgressBar
    protected static RelativeLayout relativeLayout;                 // Layout. Used to change background_color
    protected static Button button_logout;                          // Button used in order to logout

    protected void showDialog() {
        accountDialog = AccountAlertDialogFragment.newInstance();
        accountDialog.show(getFragmentManager(), "account_dialog");
    }

    protected static void showDialog(Activity activity) {
        accountDialog = AccountAlertDialogFragment.newInstance();
        accountDialog.show(activity.getFragmentManager(), "account_dialog");
    }

    public static class AccountAlertDialogFragment extends DialogFragment {
        public static AccountAlertDialogFragment newInstance() {
            return new AccountAlertDialogFragment();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            getActivity().finish();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.dialog_account, container);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            final Activity activity = getActivity();
            Button loginButton = (Button) view.findViewById(R.id.button_dialogAccount_login);
            Button registerButton = (Button) view.findViewById(R.id.button_dialogAccount_register);
            final EditText editText_existingUsername = (EditText) view.findViewById(R.id.editText_dialogAccount_existingUsername);
            final EditText editText_existingPassword = (EditText) view.findViewById(R.id.editText_dialogAccount_existingPassword);
            final EditText editText_newUsername = (EditText) view.findViewById(R.id.editText_dialogAccount_newUsername);
            final EditText editText_newPassword = (EditText) view.findViewById(R.id.editText_dialogAccount_newPassword);

            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (editText_existingUsername.getText().toString().isEmpty()) {
                        String message = "You must insert an username";
                        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    } else if (editText_existingPassword.getText().toString().isEmpty()) {
                        String message = "You must insert a password";
                        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    } else {
                        login(activity, editText_existingUsername.getText().toString(), editText_existingPassword.getText().toString());
                    }
                }
            });
            registerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (editText_newUsername.getText().toString().isEmpty()) {
                        String message = "You must insert an username";
                        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    } else if (editText_newPassword.getText().toString().isEmpty()) {
                        String message = "You must insert a password";
                        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    } else {
                        register(activity, editText_newUsername.getText().toString(), editText_newPassword.getText().toString());
                    }
                }
            });
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("[DEBUG] onCreate");


        // Initializations
        Settings.setActivityTheme(this);
        queue = Volley.newRequestQueue(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_citizen);

        preferences = getPreferences(MODE_PRIVATE);
        //String themeId = preferences.getString("application_theme", "");
        //System.out.println("[DEBUG] themeID is" + themeId);

        textView_activeCitizen_hello = (TextView) findViewById(R.id.textView_activeCitizen_hello);
        progressBar_activeCitizen_loading = (ProgressBar) findViewById(R.id.progressBar_activeCitizen_loading);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout_activeCitizen);
        button_logout = (Button) findViewById(R.id.button_activeCitizen_logout);
        logged = 0;

        // Retrieves account informations from non-volatile memory
        username = preferences.getString("username", "");
        password = preferences.getString("password", "");

        // Checks if this is an application start or a device configuration change
        if (savedInstanceState != null) {
            fromConfigurationChange = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("[DEBUG] onResume, fromConfigurationChange is " + fromConfigurationChange);
        if (fromConfigurationChange) {
            System.out.println("[DEBUG] onResume, progressBarShown is " + progressBarShown);
            if (logged == 1) {
                System.out.println("[DEBUG] onResume, sto per rendere visibile il bottone di logout");
                button_logout.setVisibility(View.VISIBLE);
            }
            if (progressBarShown == 1) {
                showProgressBar(this, true);
            }
        } else {
            // The application has been started from scratch
            if (logged == 0) {
                // Just to avoid showing progressBar in case we go back to this activity from another one
                if (username.equals("")) {
                    // The application has been just started, and no account informations exist on disk
                    showDialog();
                } else {
                    // The application has just been started, and account informations exist on disk
                    System.out.println("[DEBUG] application just started and stored data existing, I'll show the bar");
                    showProgressBar(this, true);
                    login(this, username, password);
                }
            }
        }
        // TODO rimuovere
        System.out.println("[DEBUG] onResume(), user e pass e id sono " + username + ", " + password + ", " + userId);
    }


    // TODO: i 3 successivi potro rimuoverli, alla fine
    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("[DEBUG] onPause()");
    }


    protected static void showProgressBar(Activity activity, boolean doIHaveToShow) {
        if (doIHaveToShow) {
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            relativeLayout.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), R.color.uninteractive_screen));
            progressBar_activeCitizen_loading.setVisibility(View.VISIBLE);
            progressBarShown = 1;
        } else {
            //progressBar_activeCitizen_loading = (ProgressBar) activity.findViewById(R.id.progressBar_activeCitizen_loading);
            progressBar_activeCitizen_loading.setVisibility(View.GONE);
            relativeLayout.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), android.R.color.background_light));
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            progressBarShown = 0;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("[DEBUG] onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("[DEBUG] onDestroy()");
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
            case R.id.settings:
                Intent i = new Intent(this, Settings.class);
                startActivity(i);
                return true;
            case R.id.about:
                // TODO print
                System.out.println("[DEBUG] about pressed");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO print
        System.out.println("[DEBUG] onSaveInstanceState, logged vale " + logged);
        outState.putInt("logged", logged);
        outState.putInt("userid", userId);
        outState.putString("username", username);
        outState.putString("password", password);
        outState.putInt("progressBarShown", progressBarShown);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            logged = savedInstanceState.getInt("logged", 100);
            progressBarShown = savedInstanceState.getInt("progressBarShown", 0);
            // TODO print
            System.out.println("[DEBUG] onRestoreInstanceState(), logged vale " + logged);
            userId = savedInstanceState.getInt("userid");
            username = savedInstanceState.getString("username");
            password = savedInstanceState.getString("password");
            System.out.println("[DEBUG] onRestoreInstanceState(), user e pass e id sono " + username + ", " + password + ", " + userId);
            if (!username.equals("")) {
                textView_activeCitizen_hello.setText("Welcome, " + username);
            }
        }
    }

    protected static void login(final Activity activity, final String existingUsername, final String existingPassword) {
        // TODO print
        System.out.println("[DEBUG] login");
        String url = "http://www.activecitizen.altervista.org/login/";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println("[DEBUG] login(), response is " + response);
                        if (response.compareTo("0") == 0) {
                            // TODO print
                            // An exception occurred at the server
                            System.out.println("[DEBUG] response equals 0");
                            String message = "An error occurred while contacting the server";
                            Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        } else if (response.compareTo("-1") == 0) {
                            // TODO print
                            // The select returned no entries
                            System.out.println("[DEBUG] response equals -1");
                            String message = "Invalid username or password";
                            Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            if(accountDialog == null){
                                // This can happen if account data stored on the device are no longer valid (e.g. database has been manually modified)
                                showDialog(activity);
                            }
                        } else {
                            // The inserted information were correct. The answer contains the account id
                            userId = Integer.parseInt(response);
                            username = existingUsername;
                            password = existingPassword;
                            logged = 1;
                            SharedPreferences.Editor editor = preferences.edit();
                            //editor.putInt("userid", userId);
                            editor.putString("username", username);
                            editor.putString("password", password);
                            editor.commit();
                            // TODO print
                            System.out.println("[DEBUG] login successful, user e pass e id sono " + username + ", " + password + ", " + userId + ". logged is " + logged);

                            textView_activeCitizen_hello.setText("Welcome, " + username);
                            button_logout.setVisibility(View.VISIBLE);
                            showProgressBar(activity, false);

                            if ((accountDialog != null) && (accountDialog.isResumed())) {
                                System.out.println("[DEBUG] login successful, dialog chiuso nel primo if ");
                                // the login happened by means of account dialog, that now must be closed
                                accountDialog.dismiss();
                            } else {
                                // TODO controllare se fosse sufficiente utilizzare solo la parte qui sotto
                                DialogFragment dialogFragment = (DialogFragment) activity.getFragmentManager().findFragmentByTag("account_dialog");
                                if (dialogFragment != null) {
                                    System.out.println("[DEBUG] login successful, dialog chiuso nel secondo if ");
                                    dialogFragment.dismiss();
                                }
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // GoogleVolley error, e.g. timeout happens when connection is not available
                        // TODO print
                        System.out.println("[DEBUG] onErrorResponse");
                        String message = "Connection error";
                        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        activity.finish();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", existingUsername);
                params.put("password", existingPassword);
                return params;
            }
        };
        queue.add(postRequest);
    }

    protected static void register(final Activity activity, final String newUsername, final String newPassword) {
        // TODO print
        String url = "http://www.activecitizen.altervista.org/register/";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if ((response.compareTo("0") == 0)) {
                            // An exception occurred at the server
                            System.out.println("[DEBUG] response equals 0");
                            String message = "An error occurred while contacting the server";
                            Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        } else if (response.compareTo("-1") == 0) {
                            // There already exists an account with that username
                            String message = "Username already used, choose another";
                            Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        } else {
                            // There was no account with that name. Registration succeded.
                            String message = "Account created successfully";
                            Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            userId = Integer.parseInt(response);
                            username = newUsername;
                            password = newPassword;
                            textView_activeCitizen_hello.setText("Welcome, " + username);
                            button_logout.setVisibility(View.VISIBLE);
                            SharedPreferences.Editor editor = preferences.edit();
                            //editor.putInt("userid", userId);
                            editor.putString("username", username);
                            editor.putString("password", password);
                            editor.commit();
                            // TODO print
                            if ((accountDialog != null) && (accountDialog.isResumed())) {
                                System.out.println("[DEBUG] registration successful, dialog chiuso nel primo if ");
                                // the login happened by means of account dialog, that now must be closed
                                accountDialog.dismiss();
                            } else {
                                // TODO controllare se fosse sufficiente utilizzare solo la parte qui sotto
                                DialogFragment dialogFragment = (DialogFragment) activity.getFragmentManager().findFragmentByTag("account_dialog");
                                if (dialogFragment != null) {
                                    System.out.println("[DEBUG] registration successful, dialog chiuso nel secondo if ");
                                    dialogFragment.dismiss();
                                }
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // GoogleVolley error, e.g. timeout happens when connection is not available
                        String message = "Connection error";
                        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        activity.finish();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", newUsername);
                params.put("password", newPassword);
                return params;
            }
        };
        queue.add(postRequest);
    }

    public void launchReportAnIssue(View v) {
        if (userId == 0) {
            Toast.makeText(getApplicationContext(), "Account error", Toast.LENGTH_LONG).show();
            finish();
        }
        // TODO print
        System.out.println("[DEBUG] Sending an intent to ReportAnIssue");
        Intent i = new Intent(this, ReportAnIssue.class);
        i.putExtra("user_id", userId);
        startActivity(i);
    }

    public void launchBrowseMap(View v) {
        if (userId == 0) {
            Toast.makeText(getApplicationContext(), "Account error", Toast.LENGTH_LONG).show();
            finish();
        }
        System.out.println("[DEBUG] Sending an intent to BrowseMap");
        Intent i = new Intent(this, BrowseMap.class);
        i.putExtra("user_id", userId);
        startActivity(i);
    }

    public void logout(View v) {
        // TODO print
        System.out.println("[DEBUG] logout pressed");
        userId = 0;
        username = "";
        password = "";
        button_logout.setVisibility(View.GONE);
        textView_activeCitizen_hello.setText("");
        SharedPreferences.Editor editor = preferences.edit();
        //editor.putInt("userid", 0);
        editor.putString("username", "");
        editor.putString("password", "");
        editor.commit();
        showDialog();
        logged = 0;
    }
}
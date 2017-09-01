package com.example.alessandro.activecitizen;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Created by Alessandro on 31/08/2017.
 */

public class Settings extends PreferenceActivity {

    public static void setActivityTheme(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String themeId = sharedPref.getString("application_theme", "");

        if(themeId.equals("1")) {
            context.setTheme(R.style.ClassicTheme);
            System.out.println("[DEBUG] themeID is " + themeId + ", so ClassicTheme!");
        } else if(themeId.equals("2")){
            System.out.println("[DEBUG] themeID is " + themeId + ", so ActiveCitizenTheme!");
            context.setTheme(R.style.ActiveCitizenTheme);
        } else if(themeId.equals("3")){
            System.out.println("[DEBUG] themeID is " + themeId + ", so DarkTheme!");
            context.setTheme(R.style.Theme_AppCompat_DayNight);
        }
    }

    public static boolean getConfirmExit(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("confirm_report_exit", true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setActivityTheme(this);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_settings);
        // TODO: quando si cambia tema, ci deve venire scritto "riavvia l'applicazione affinche'
        // le modifiche abbiano effetto. O meglio "le modifiche avranno effetto al prossimo riavvio".
    }
}

package com.example.alessandro.activecitizen;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class ActiveCitizen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_citizen);
    }

    public void reportAnIssue(View v) {
        System.out.println("[DEBUG] Mando un intent a ReportAnIssue");
        Intent i = new Intent(this, ReportAnIssue.class);
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
                return true;
            case R.id.settings:
                System.out.println("[DEBUG] settings pressed");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

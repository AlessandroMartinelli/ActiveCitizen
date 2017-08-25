package com.example.alessandro.activecitizen;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.R.id.message;
import static com.example.alessandro.activecitizen.R.id.username;
import static java.lang.Integer.parseInt;

/**
 * Created by Alessandro on 06/08/2017.
 */

public class ManualCoordinates extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, LocationListener {

    private LocationManager locationManager;
    private GoogleMap gmap;
    private LatLng manuallySelectedCoordinates;
    private LatLng currentCoordinates;
    private String provider;
    private Criteria criteria;

    private LinearLayout ll_confirm;
    private LinearLayout ll_button;

    private boolean manually_select_coordinates;

    private int userId;
    private RequestQueue queue;
    private String url;
    private ProgressDialog loadingDialog;
    private ReportList reportList;

    private AlertDialog reportDialog;

    public Bitmap StringToBitMap(String encodedString){
        try {
            byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    protected void retrieveReportDetails(final Report r){
        System.out.println("[DEBUG] inside retrieveReportDetails_v1 for " + r.reportTitle);
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("0")) {
                            System.out.println("[DEBUG] response is 0 :c");
                        } else {
                            //System.out.println("[DEBUG] there is a response! c:");
                            //System.out.println("[DEBUG] raw response: " + response);
                            //response = response.substring(1, (response.length() - 1));
                            //System.out.println("[DEBUG] modified response: " + response);

                            try {
                                JSONObject jObject = new JSONObject("{output:" + response + "}");
                                System.out.println("[DEBUG] first step passed");
                                response = jObject.getString("output");
                                //System.out.println("[DEBUG] parsed string is " + response);
                            }
                            catch (org.json.JSONException e){
                                e.printStackTrace();
                                //e.printStackTrace(System.out);
                                //System.out.println("[DEBUG] exception: " + e.getStackTrace().);
                                //System.out.println("[DEBUG] exception: " + e.getMessage());
                                //System.out.println("[DEBUG] exception: " + e.getCause().toString());
                            }


                            System.out.println("[DEBUG] sto per splittare la stringa");
                            String[] field = response.split("~");
                            r.reportDescription = field[0];
                            System.out.println("[DEBUG] report description is " + r.reportDescription);
                            System.out.println("[DEBUG] recovered imageString length is " + field[1].length());
                            Bitmap b = StringToBitMap(field[1]);
                            if(b == null){
                                System.out.println("[DEBUG] Error in decoding the image");
                            } else {
                                System.out.println("[DEBUG] Images correctly decoded");
                                r.reportImage = b;
                            }
                            showReportDialog(r);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("[DEBUG] onErrorResponse. Error is " + error.getMessage());
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("action", "get_report_details");
                params.put("report_id", "" + r.reportId);
                return params;
            }
        };
        queue.add(postRequest);
    }


    private class ReportList{
        protected ArrayList<Report> reportList;

        public ReportList(){
            reportList = new ArrayList<Report>();
        }

        public void addReport(Report report){
            System.out.println("[DEBUG] sono dentro la addReport, report vale " + report.toString());
            if(report == null){
                System.out.println("[DEBUG] report e' null, quindi non chiamo la add di ArrayList");
            } else {
                System.out.println("[DEBUG] reportList vale null? " + (reportList == null? "yep" : "nope"));
                reportList.add(report);
            }
        }

        public void printFirst(){
            System.out.println("[DEBUG] sono dentro la printFirst");
            Report r = reportList.get(0);
            if(r == null){
                System.out.println("[DEBUG] report in printFirst() e' null, quindi non chiamo la print");
            }
            System.out.println("[DEBUG] Object #0 is: " +
                    "username " + r.username + ", " +
                    "authorId " + r.authorId + ", " +
                    "reportId " + r.reportId + ", " +
                    "reportTitle " + r.reportTitle + ", " +
                    "coordinates " + r.coordinates.latitude + ", " + r.coordinates.longitude);
        }
    }

    private class Report{
        protected String username;
        protected int authorId;
        protected int reportId;
        protected String reportTitle;
        protected LatLng coordinates;
        protected Float avgRating;
        protected String reportDescription;
        protected Bitmap reportImage;

        public Report(String username, int authorId, int reportId, String reportTitle,
                           LatLng coordinates, Float avgRating){
            this.username = username;
            this.authorId = authorId;
            this.reportId = reportId;
            this.reportTitle = reportTitle;
            this.coordinates = coordinates;
            this.avgRating = avgRating;
        }

        public String toString(){
            String s = "username " + username + "\n" +
                    "authorId " + authorId + "\n" +
                    "reportId " + reportId + "\n" +
                    "reportTitle " + reportTitle + "\n" +
                    "coordinates " + coordinates.latitude + ", " + coordinates.longitude + "\n" +
                    "avgRating " + avgRating;
            return s;
        }

        // a report.show() method would be useful in order to show a popup
        // with all information abount this report.
    }

    protected void showReportDialog(Report report) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View inflater = layoutInflater.inflate(R.layout.dialog_report, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(inflater);
        //builder.setCancelable(false);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                reportDialog.dismiss();
            }
        });
        reportDialog = builder.show();

        TextView title = (TextView) inflater.findViewById(R.id.textView_dialogReport_title);
        TextView category = (TextView) inflater.findViewById(R.id.textView_dialogReport_category);
        TextView details = (TextView) inflater.findViewById(R.id.textView_dialogReport_details);
        ImageView image = (ImageView) inflater.findViewById(R.id.imageView_dialogReport_image);
        RatingBar rating = (RatingBar) inflater.findViewById(R.id.ratingBar_dialogReport_rating);

        title.setText(report.reportTitle);
        details.setText(report.reportDescription);
        image.setImageBitmap(report.reportImage);
        rating.setRating(report.avgRating);

        /*
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
        */
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Intent startingIntent = getIntent();
        userId = startingIntent.getIntExtra("user_id", 0);
        if(userId == 0){
            Toast.makeText(getApplicationContext(), "Account error", Toast.LENGTH_LONG).show();
            finish();
        }

        if(getCallingActivity() == null){
            // The activity was started through "browse map"
            manually_select_coordinates = false;
            setContentView(R.layout.activity_manual_coordinates);
            queue = Volley.newRequestQueue(this);
            url = "http://www.activecitizen.altervista.org";
            reportList = new ReportList();
            get_report_index();
        } else {
            // The activity was started through "manually select coordinates"
            manually_select_coordinates = true;
            setContentView(R.layout.activity_manual_coordinates);
            ll_confirm = (LinearLayout) findViewById(R.id.linearLayout_confirm);
            ll_button = (LinearLayout) findViewById(R.id.linearLayout_button);
        }

        if(savedInstanceState == null){
            // The activity has been started, this is not a rotation
            ((MapFragment) getFragmentManager().findFragmentById(R.id.coordinates_map)).
                    getMapAsync(this);
            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            provider = locationManager.getBestProvider(criteria, true);
            if(provider.isEmpty()){
            } else {
                locationManager.requestSingleUpdate(provider, this, null);
            }
        }

    }

    @Override
    public void onLocationChanged(Location location){
        System.out.println("[DEBUG] onLocationChanged inizia qua");
        currentCoordinates = new LatLng (location.getLatitude(), location.getLongitude());
        if(gmap != null) {
            gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, 15));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        if(manually_select_coordinates == true) {
            gmap.setOnMapClickListener(this);
        }
        centerMap(null);
        System.out.println("[DEBUG] map type is " + gmap.getMapType());
        String message = "Wait a few seconds for the map to be centered...";
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        //getUiSettings
        UiSettings settings = gmap.getUiSettings();
        settings.setZoomControlsEnabled(true);
    }

    @Override
    public void onMapClick(LatLng clickedCoordinates){
        manuallySelectedCoordinates = clickedCoordinates;

        gmap.clear();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(manuallySelectedCoordinates);
        gmap.addMarker(markerOptions);


        TextView tv = (TextView) findViewById(R.id.textView_coordinatesToBeConfirmed);
        tv.setText(manuallySelectedCoordinates.latitude + ", " +
                manuallySelectedCoordinates.longitude);

        ll_button.setVisibility(View.GONE);
        ll_confirm.setVisibility(View.VISIBLE);
    }

    public void centerMap(View v){
        locationManager.requestSingleUpdate(provider, this, null);
    }

    public void ok(View v){
        if(manuallySelectedCoordinates == null){
            // TODO: as of now, 2017/08/21, this branch should be never accessed
            // String message = "You must first select a location";
            // Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("coordinates", manuallySelectedCoordinates);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
    }


    protected void get_report_index() {
        System.out.println("[DEBUG] inside the get_report_index");
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        System.out.println("[DEBUG] get_report_index response is " + response);
                        if(response.equals("0")){
                            String message = "No report to retrieve";
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            if(loadingDialog != null && loadingDialog.isShowing()){
                                loadingDialog.dismiss();
                            }
                        } else {
                            response = response.substring(1, (response.length() - 1));
                            String[] reports = response.split("=");
                            int reportNum = reports.length;
                            for(int i=0; i<reportNum; i++){
                                String[] reportField = reports[i].split("~");
                                /*
                                int authorIdActual = Integer.parseInt(authorId);
                                int reportIdActual = Integer.parseInt(reportId);
                                Double latitudeActual = Double.parseDouble(latitude);
                                Double longitudeActual = Double.parseDouble(longitude);
                                LatLng coordinates = new LatLng(Double.parseDouble(reportField[4]),
                                    Double.parseDouble(reportField[5])));
                                */


                                Report newReport = new Report(
                                        reportField[0],
                                        Integer.parseInt(reportField[1]),
                                        Integer.parseInt(reportField[2]),
                                        reportField[3],
                                        new LatLng(Double.parseDouble(reportField[4]),
                                                Double.parseDouble(reportField[5])),
                                        Float.parseFloat(reportField[6]));

                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(newReport.coordinates);
                                markerOptions.title(newReport.reportTitle);
                                markerOptions.snippet("Author: " + newReport.username + ". " +
                                        "Avg rate: " + newReport.avgRating);
                                if(newReport.authorId == userId){
                                    markerOptions.icon(BitmapDescriptorFactory.
                                            defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                }
                                Marker marker = gmap.addMarker(markerOptions);
                                marker.setTag(newReport);
                                gmap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
                                    @Override
                                    public void onInfoWindowClick(Marker marker) {
                                        Report r = (Report)marker.getTag();
                                        System.out.println("[DEBUG] Hey, you have pressed on an infoWindow! reportTitle is " + r.reportTitle);
                                        if(r.reportImage != null){
                                            System.out.println("[DEBUG] reportImage already stored, no need to download it again");
                                            // r.showDetailedView();
                                        } else {
                                            // Request the full details to the server
                                            System.out.println("[DEBUG] I'm going to retrieve the details of " + r.reportTitle);
                                            // TODO qui forse dovrei passare anche il marker, così che, se opportuno, venga colorato
                                            retrieveReportDetails(r);
                                        }
                                    }
                                });


                                // reportList.addReport(newReport);
                            }

                            // reportList.printFirst();
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
                params.put("action", "get_report_index");
                return params;
            }
        };
        queue.add(postRequest);
    }


    //TODO: onback, se sta venendo mostrato il popup, va nascosto.

    @Override
    public void onBackPressed(){
        if(manually_select_coordinates == true){
            if(ll_confirm.getVisibility() == View.VISIBLE){
                gmap.clear();
                ll_confirm.setVisibility(View.GONE);
                ll_button.setVisibility(View.VISIBLE);
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }

    }


    @Override
    public void onProviderDisabled(String provider){ }
    @Override
    public void onProviderEnabled(String provider){ }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }
}


//android:icon="@android/drawable/..


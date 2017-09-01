// TODO nella onPause devo togliere eventuali richieste di posizione pendenti
// TODO gestire il tasto back quando c'e' un marker aperto, in tal caso bisognerebbe semplicemente chidere il marker, non l'intera applicazione

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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.R.id.message;
import static java.lang.Integer.parseInt;

/**
 * Created by Alessandro on 06/08/2017.
 */

public class BrowseMap extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, LocationListener {

    protected LocationManager locationManager;          // TODO questo penso non serva più, idem i provider e i criteria
    protected GoogleMap gmap;
    protected LatLng manuallySelectedCoordinates;
    protected LatLng currentCoordinates;
    protected String provider;
    protected Criteria criteria;

    protected boolean manually_select_coordinates;      // true if the activity has been started from ReportAnIssuue activity

    protected int userId;
    protected RequestQueue queue;
    protected ProgressDialog loadingDialog;
    protected ProgressBar progressBar_loading;          // Loading ProgressBar
    protected int progressBarShown;
    //private ReportList reportList;

    protected AlertDialog reportDialog;
    protected RelativeLayout relativeLayout;


    /**
     * Retrieve the Bitmap codified in the given String
     * @param encodedString the string encoding a Bitmap
     * @return the Bitmap codified in the given String
     */
    protected Bitmap StringToBitMap(String encodedString){
        try {
            byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    protected void showProgressBar(Activity activity, boolean doIHaveToShow) {
        if(doIHaveToShow) {
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            relativeLayout.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), R.color.uninteractive_screen));
            progressBar_loading.setVisibility(View.VISIBLE);
            progressBarShown = 1;
        } else {
            //progressBar_activeCitizen_loading = (ProgressBar) activity.findViewById(R.id.progressBar_activeCitizen_loading);
            progressBar_loading.setVisibility(View.GONE);
            relativeLayout.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), android.R.color.background_light));
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            progressBarShown = 0;
        }
    }

    /**
     * Utility method used for retrieving the String corresponding
     * to the category choosen by means of the spinner.
     * Case 0 means no choice has been done.
     *
     * @param categoryIndex an index representing the choosen category
     * @return return a string representing the category
     */
    protected String categoryIndexToString(int categoryIndex){
        switch (categoryIndex){
            case 0:
                return "";
            case 1:
                return "Viability";
            case 2:
                return "Security";
            case 3:
                return "Public lighting";
            case 4:
                return "Buildings";
            case 5:
                return "Decay";
            case 6:
                return "Public health";
            case 7:
                return "Other";
            default:
                return "";
        }
    }

    protected void getReportDetails(final Marker marker){
        final Report r = (Report)marker.getTag();
        String url = "http://www.activecitizen.altervista.org/get_report_details/";
        System.out.println("[DEBUG] inside retrieveReportDetails_v1 for " + r.reportTitle +
                "; userId is " + userId + "; your_rate is " + r.your_rate);
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("0")) {
                            String message = "An error occurred while contacting the server";
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        } else if(response.equals("-1")) {
                            String message = "Error in retrieving the report";
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                JSONObject jObject = new JSONObject("{output:" + response + "}");
                                response = jObject.getString("output");
                            }
                            catch (org.json.JSONException e){
                                String message = "Error while decoding the report";
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            }
                            String[] field = response.split("~");
                            r.reportAuthor = field[0];
                            r.reportDate = field[1];
                            r.reportDetails = field[2];
                            Bitmap b = StringToBitMap(field[3]);
                            if(b != null){
                                r.reportImage = b;
                            }
                            showReportDialog(marker);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String message = "Connection error";
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("report_id", "" + r.reportId);
                return params;
            }
        };
        queue.add(postRequest);
    }


    /*
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
    */

    protected class Report{
        // dovrei aggiungere dei getter e dei setter per i campi non inizializzati nel costruttore
        protected String reportAuthor;
        protected int authorId;
        protected int reportId;
        protected String reportTitle;
        protected int reportCategory;
        protected String reportDate;
        protected String reportDetails;
        protected LatLng coordinates;
        protected Bitmap reportImage;
        protected Float avgRating;
        protected Float your_rate;

        public Report(int authorId, int reportId, String reportTitle, int reportCategory,
                      LatLng coordinates, Float avgRating, Float your_rate){
            this.authorId = authorId;
            this.reportId = reportId;
            this.reportTitle = reportTitle;
            this.reportCategory = reportCategory;
            this.coordinates = coordinates;
            this.avgRating = avgRating;
            this.your_rate = your_rate;
        }

        public String toString(){
            String s = "authorId " + authorId + "\n" +
                    "reportId " + reportId + "\n" +
                    "reportTitle " + reportTitle + "\n" +
                    "reportCategory " + reportCategory + "\n" +
                    "coordinates " + coordinates.latitude + ", " + coordinates.longitude + "\n" +
                    "avgRating " + avgRating;
            return s;
        }

        // a report.show() method would be useful in order to show a popup
        // with all information abount this report.
    }

    protected void showReportDialog(final Marker marker) {
        final Report report = (Report)marker.getTag();
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
        TextView author_and_date = (TextView) inflater.findViewById(R.id.textView_dialogReport_author_and_date);
        TextView details = (TextView) inflater.findViewById(R.id.textView_dialogReport_details);
        ImageView image = (ImageView) inflater.findViewById(R.id.imageView_dialogReport_image);
        final RatingBar rating = (RatingBar) inflater.findViewById(R.id.ratingBar_dialogReport_rating);
        Button buttonRate = (Button) inflater.findViewById(R.id.button_ratingBar_rate);

        title.setText(report.reportTitle);
        category.setText("Category: " + categoryIndexToString(report.reportCategory));
        author_and_date.setText("Reported by " + report.reportAuthor + " on " +
                //report.reportDate.getYear() + "-" + report.reportDate.getMonth() +
                //"-" + report.reportDate.getDay());
                report.reportDate);
        details.setText(report.reportDetails);
        image.setImageBitmap(report.reportImage);
        // TODO: qui dovrei infilare un textview che riporta il voto medio
        rating.setRating(report.your_rate);

        buttonRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int the_rate_is_new = (report.your_rate == 0? 1 : 0);
                System.out.println("[DEBUG]: the_rate_is_new vale " + the_rate_is_new);
                url = "http://www.activecitizen.altervista.org/rate/";
                //System.out.println("[DEBUG] inside the send rate, rate is " + rating.getRating() +
                  //      ", old_rate is " + old_rate);
                StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                if (response.equals("-1")) {
                                    System.out.println("[DEBUG] response is 0 :c");
                                } else {
                                    report.your_rate = rating.getRating();
                                    if(report.authorId != userId){
                                        // If the report has been issued by the user,
                                        // its color its already HUE_AZURE, no need
                                        // to modifying it
                                        marker.setIcon(BitmapDescriptorFactory.
                                                defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                                    }
                                    System.out.println("[DEBUG] response is " + response);
                                    String message = "Report voted successfully";
                                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                    //TODO qui si deve poi fare il dismiss del dialog
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
                        params.put("user_id", "" + userId);
                        params.put("report_id", "" + report.reportId);
                        params.put("rate", ""  + rating.getRating());
                        params.put("old_rate", "" + report.your_rate);
                        params.put("the_rate_is_new", "" + the_rate_is_new);
                        return params;
                    }
                };
                queue.add(postRequest);
            }
        });

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
        Settings.setActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_map);

        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout_browseMap);

        if(savedInstanceState == null){
            // The activity has been started, this is not a rotation, since
            // we do not want to ask the current position at each rotation
            ((MapFragment) getFragmentManager().findFragmentById(R.id.coordinates_map)).
                    getMapAsync(this);
            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            provider = locationManager.getBestProvider(criteria, true);
            /*
            if(provider.isEmpty()){
                // TODO: ???
            } else {
                locationManager.requestSingleUpdate(provider, this, null);
            }
            */
        }

        if(getCallingActivity() == null){
            // The activity was started through "browse map" from the main screen
            manually_select_coordinates = false;

            Intent startingIntent = getIntent();
            userId = startingIntent.getIntExtra("user_id", 0);
            if(userId == 0){
                Toast.makeText(getApplicationContext(), "Account error", Toast.LENGTH_LONG).show();
                finish();
            }
            queue = Volley.newRequestQueue(this);
            getReportIndex();
        } else {
            // The activity was started through "manually select coordinates"
            manually_select_coordinates = true;
            //ll_confirm = (LinearLayout) findViewById(R.id.linearLayout_confirm);
            //ll_button = (LinearLayout) findViewById(R.id.linearLayout_button);
        }

    }

    @Override
    public void onLocationChanged(Location location){
        System.out.println("[DEBUG] onLocationChanged inizia qua");
        currentCoordinates = new LatLng (location.getLatitude(), location.getLongitude());
/*
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(currentCoordinates);
        circleOptions.radius(20);
        circleOptions.fillColor(0xff7ae6ff);
        gmap.addCircle(circleOptions);

        CircleOptions circleOptions2 = new CircleOptions();
        circleOptions2.center(currentCoordinates);
        circleOptions2.radius(20);
        circleOptions2.fillColor(0x007ae6ff);
        gmap.addCircle(circleOptions2);
*/
/*
        CircleOptions circleOptions3 = new CircleOptions();
        circleOptions3.center(currentCoordinates);
        circleOptions3.radius(20);
        circleOptions3.fillColor(0xff35bfdd);
        circleOptions3.strokeColor(0xffffffff);
        gmap.addCircle(circleOptions3);
*/
        gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, 15));

        // TODO: penso che dovrei fare la seguente cosa: in manual coordinates, non interessa la
        // posizione corrente: stiamo usando tale metodo invece che "retrieve current location"
        // proprio perche' la segnalazione non e' nella posizione corrente.
        //Caprioli

    }

    public void commuteTracking(View v){
        Button button = (Button)findViewById(R.id.button_browseMap_track);
        if(!gmap.isMyLocationEnabled()) {
            gmap.setMyLocationEnabled(true);
            button.setText("Stop Tracking");
        } else {
            gmap.setMyLocationEnabled(false);
            button.setText("Track");
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        if(manually_select_coordinates == true) {
            gmap.setOnMapClickListener(this);

            /*
            gmap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    marker.showInfoWindow();
                    return true;
                }
            });
            */

            gmap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    ok(null);
                }
            });
        }
        //getUiSettings
        UiSettings settings = gmap.getUiSettings();
        settings.setZoomControlsEnabled(true);
        // TODO check permission

    }

    @Override
    public void onMapClick(LatLng clickedCoordinates){
        // TODO centrare la mappa sulle coordinate corrent
        manuallySelectedCoordinates = clickedCoordinates;
        gmap.clear();
        gmap.animateCamera(CameraUpdateFactory.newLatLng(manuallySelectedCoordinates));
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(manuallySelectedCoordinates);
        markerOptions.title("click here to confirm coordinates");
        markerOptions.snippet(manuallySelectedCoordinates.latitude + ", " +
                manuallySelectedCoordinates.longitude);
        Marker marker = gmap.addMarker(markerOptions);
        marker.showInfoWindow();

        // TODO c'e' da chiamare "ok"

        /*
        TextView tv = (TextView) findViewById(R.id.textView_coordinatesToBeConfirmed);
        tv.setText(manuallySelectedCoordinates.latitude + ", " +
                manuallySelectedCoordinates.longitude);
        ll_button.setVisibility(View.GONE);
        ll_confirm.setVisibility(View.VISIBLE);
        */
    }

    public void centerMap(View v){
        locationManager.requestSingleUpdate(provider, this, null);
        String message = "Wait a few seconds for the map to be centered...";
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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

    //TODO: penso che lo zoom automatico sia sbagliato: magari uno e' li che sta
    // selezionando minuziosamente una zona, e poi lo zoom automatico gli stravolge tutto

    //TODO: al momento, se non ci sono segnalazioni, crasha.
    protected void getReportIndex() {
        // TODO qui potrei fare una progress bar che raggiunge il 50% dopo aver scaricato
        // tutto e poi sale gradualmente.
        url= "http://www.activecitizen.altervista.org/get_report_index/";
        System.out.println("[DEBUG] inside the getReportIndex");
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("log", "[DEBUG] getReportIndex response is " + response);
                        //System.out.println("[DEBUG] getReportIndex response is " + response);
                        // TODO: ricordarsi di gestire in maniera uniforme gli errori, in tutte le activity
                        if(response.equals("0")){
                            String message = "No report to retrieve";
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            if(loadingDialog != null && loadingDialog.isShowing()){
                                loadingDialog.dismiss();
                            }
                        } else {
                            try {
                                //System.out.println("[DEBUG] inside the try block");
                                JSONObject jObject = new JSONObject("{output:" + response + "}");
                                //System.out.println("[DEBUG] first step passed");
                                response = jObject.getString("output");
                                //System.out.println("[DEBUG] response is now " + response);
                                //System.out.println("[DEBUG] parsed string is " + response);
                            }
                              catch (org.json.JSONException e){
                                e.printStackTrace();
                                //e.printStackTrace(System.out);
                                //System.out.println("[DEBUG] exception: " + e.getStackTrace().);
                                //System.out.println("[DEBUG] exception: " + e.getMessage());
                                //System.out.println("[DEBUG] exception: " + e.getCause().toString());
                            }


                            String[] reports = response.split("=");
                            int reportNum = reports.length;
                            for(int i=0; i<reportNum; i++){
                                String[] reportField = reports[i].split("~");

                                Report newReport = new Report(
                                        Integer.parseInt(reportField[0]), //authorId
                                        Integer.parseInt(reportField[1]), //reportId
                                        reportField[2],                   //title
                                        Integer.parseInt(reportField[3]), //category
                                        new LatLng(Double.parseDouble(reportField[4]),  //coordiates
                                                Double.parseDouble(reportField[5])),
                                        Float.parseFloat(reportField[6]),   //avgRating
                                        Float.parseFloat(reportField[7]));  //yourRate
                                System.out.println("[DEBUG] per il marker " +
                                        newReport.reportTitle + " your_rate vale " + newReport.your_rate);

                                //TODO: alla risposta qui data, bisogna concatenare i dati riguardanti
                                // quali report ha votato l'utente (magari senza includere i suoi)

                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(newReport.coordinates);
                                markerOptions.title(newReport.reportTitle);
                                markerOptions.snippet("Category: " +
                                        categoryIndexToString(newReport.reportCategory) +
                                        ". Avg rate: " + newReport.avgRating);
                                if(newReport.authorId == userId){
                                    markerOptions.icon(BitmapDescriptorFactory.
                                            defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                    // TODO: qui bisogna aggiungere "altrimenti: se l'hai votato, colore diverso"
                                    System.out.println("[DEBUG] per questo marker, your_rate vale " + newReport.your_rate);
                                } else if(newReport.your_rate != 0){
                                    // The user has already votes this issue
                                    markerOptions.icon(BitmapDescriptorFactory.
                                            defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                                }
                                Marker marker = gmap.addMarker(markerOptions);
                                marker.setTag(newReport);
                                gmap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
                                    @Override
                                    public void onInfoWindowClick(Marker marker) {
                                        System.out.println("[DEBUG] onInfoWindowclick called on marker " + marker.getId());
                                        Report r = (Report)marker.getTag();
                                        System.out.println("[DEBUG] Hey, you have pressed on an infoWindow! reportTitle is " + r.reportTitle);
                                        if(r.reportImage != null){
                                            System.out.println("[DEBUG] reportImage already stored, no need to download it again");
                                            // TODO: r.showDetailedView();
                                            showReportDialog(marker);
                                        } else {
                                            // Request the full details to the server
                                            System.out.println("[DEBUG] I'm going to retrieve the details of " + r.reportTitle);
                                            // TODO qui forse dovrei passare anche il marker, così che, se opportuno, venga colorato
                                            getReportDetails(marker);
                                        }
                                    }
                                });

                                // TODO: aggiornare i valori subito dopo averli modificati


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
                params.put("user_id", "" + userId);
                return params;
                // TODO: provare a vedere se restituire null funziona lo stesso
            }
        };
        queue.add(postRequest);
    }


    //TODO: onback, se sta venendo mostrato il popup, va nascosto.

    /*
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
    */


    @Override
    public void onProviderDisabled(String provider){ }
    @Override
    public void onProviderEnabled(String provider){ }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }
}


//android:icon="@android/drawable/..

//TODO: back non sta facendo il suo dovere di, se un marker ha la sua finestrella visualizzata, toglierla e basta

//quando uno clicca per vedere i dettagli di un report, la finestrella relativa al marker relativo dovrebbe scomparire, cosi' che se uno fa indietro non la vede

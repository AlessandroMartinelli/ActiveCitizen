// TODO al momento, se si ruota, cicla all'infinito.

// TODO nella onPause devo togliere eventuali richieste di posizione pendenti
// TODO gestire il tasto back quando c'e' un marker aperto, in tal caso bisognerebbe semplicemente chidere il marker, non l'intera applicazione

// TODO forse, quando "track" e' attivo, periodicamente bisognerebbe centrare la mappa sulla posizione dell'utente.

package com.example.alessandro.activecitizen;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;

import org.json.JSONObject;

import android.Manifest;

import java.util.HashMap;
import java.util.Map;

import static com.example.alessandro.activecitizen.ReportAnIssue.LOCATION_PERMISSION;
import static java.lang.Integer.parseInt;

/**
 * Created by Alessandro on 06/08/2017.
 */

public class BrowseMap extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    protected LocationManager locationManager;          // TODO questo penso non serva più, idem i provider e i criteria
    protected GoogleMap gmap;
    protected LatLng manuallySelectedCoordinates;
    protected LatLng currentCoordinates;
    protected String provider;
    protected Criteria criteria;

    protected boolean manuallySelectCoordinates;      // true if the activity has been started from ReportAnIssuue activity
    protected boolean fromConfigurationChange;

    protected static int userId;                        // TODO è proprio necessario che sia statico?
    protected static RequestQueue queue;                // TODO è proprio necessario che sia statico?
    //protected ProgressDialog loadingDialog;
    protected ProgressBar progressBar_loading;          // Loading ProgressBar
    protected int progressBarShown;
    protected String reportsIndex;
    //private ReportList reportList;

    //protected AlertDialog reportDialog;
    protected RelativeLayout relativeLayout;
    protected static ReportDialogFragment reportDialog;

    public static class ReportDialogFragment extends DialogFragment {
        static Marker marker;

        public static ReportDialogFragment newInstance(Marker currentMarker) {
            marker = currentMarker;
            return new ReportDialogFragment();
        }
        // TODO questo dovrò implementarlo?
        /*
        @Override
        public void onCancel(DialogInterface dialog) {
            getActivity().finish();
        }
        */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.dialog_report, container);
        }
        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            final Activity activity = getActivity();
            final Report report = (Report) marker.getTag();

            final TextView title = (TextView) view.findViewById(R.id.textView_dialogReport_title);
            final TextView category = (TextView) view.findViewById(R.id.textView_dialogReport_category);
            final TextView author_and_date = (TextView) view.findViewById(R.id.textView_dialogReport_author_and_date);
            final TextView details = (TextView) view.findViewById(R.id.textView_dialogReport_details);
            final ImageView image = (ImageView) view.findViewById(R.id.imageView_dialogReport_image);
            final RatingBar averageRate = (RatingBar) view.findViewById(R.id.ratingBar_dialogReport_averageRate);
            final RatingBar yourRate = (RatingBar) view.findViewById(R.id.ratingBar_dialogReport_yourRate);
            final Button buttonRate = (Button) view.findViewById(R.id.button_ratingBar_rate);

            title.setText(report.reportTitle);
            category.setText("Category: " + categoryIndexToString(report.reportCategory));
            author_and_date.setText("Reported by " + report.reportAuthor + " on " + report.reportDate);
            details.setText(report.reportDetails);
            image.setImageBitmap(report.reportImage);
            averageRate.setRating(report.avgRating);
            yourRate.setRating(report.your_rate);

            buttonRate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int the_rate_is_new = (report.your_rate == 0 ? 1 : 0);
                    System.out.println("[DEBUG]: the_rate_is_new vale " + the_rate_is_new);
                    String url = "http://www.activecitizen.altervista.org/rate/";
                    //System.out.println("[DEBUG] inside the send rate, rate is " + rating.getRating() +
                    //      ", old_rate is " + old_rate);
                    StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    if (response.equals("-1")) {
                                        System.out.println("[DEBUG] response is 0 :c");
                                        reportDialog.dismiss();
                                    } else {
                                        report.your_rate = yourRate.getRating();
                                        if (report.authorId != userId) {
                                            // If the report has been issued by the user,
                                            // its color its already HUE_AZURE, no need
                                            // to modifying it
                                            marker.setIcon(BitmapDescriptorFactory.
                                                    defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                                            reportDialog.dismiss();
                                        }
                                        System.out.println("[DEBUG] response is " + response);
                                        String message = "Report voted successfully";
                                        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                        //TODO qui si deve poi fare il dismiss del dialog
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    System.out.println("[DEBUG] onErrorResponse. Error is " + error.getMessage());
                                    reportDialog.dismiss();
                                }
                            }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("user_id", "" + userId);
                            params.put("report_id", "" + report.reportId);
                            params.put("rate", "" + yourRate.getRating());
                            params.put("old_rate", "" + report.your_rate);
                            params.put("the_rate_is_new", "" + the_rate_is_new);
                            return params;
                        }
                    };
                    queue.add(postRequest);
                }
            });
        }
    }


    protected class Report{
        // TODO dovrei aggiungere dei getter e dei setter per i campi non inizializzati nel costruttore
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

    @Override
    protected void onCreate(Bundle savedInstanceState){
        // Layout initialization
        Settings.setActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_map);
        progressBar_loading = (ProgressBar) findViewById(R.id.progressBar_browseMap_loading);
        System.out.println("[DEBUG] onCreate(), progressBar_loading is " + progressBar_loading);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout_browseMap);
        ((MapFragment) getFragmentManager().findFragmentById(R.id.coordinates_map)).
                getMapAsync(this);
        queue = Volley.newRequestQueue(this);

        if(savedInstanceState != null){
            // The activity has been recreated after a configuration change, i.e. a screen rotation
            fromConfigurationChange = true;
        } else {
            fromConfigurationChange = false;
            if(getCallingActivity() == null) {
                // The activity was started through "browse map" from the main screen
                manuallySelectCoordinates = false;
                Intent startingIntent = getIntent();
                userId = startingIntent.getIntExtra("user_id", 0);
                reportsIndex = "";      // TODO onSave and onRestore
            } else {
                // The activity was started through "manually select coordinates" for result
                manuallySelectCoordinates = true;
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        //if(progressBarShown == 1){
        //    if(!(progressBar_loading.isShown()))
            // solo se non sta venendo mostrata in questo momento
        //    showProgressBar(true);
        //}
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        System.out.println("[DEBUG] onSave, reportsIndex length is " + reportsIndex.length());
        outState.putString("reportsIndex", reportsIndex);
        outState.putInt("userId", userId);
        outState.putBoolean("manuallySelectCoordinates", manuallySelectCoordinates);
        outState.putInt("progressBarShown", progressBarShown);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            System.out.println("[DEBUG] onRestore, savedInstanceState not null");
            reportsIndex = savedInstanceState.getString("reportsIndex", "");
            userId = savedInstanceState.getInt("userId", 0);
            manuallySelectCoordinates = savedInstanceState.getBoolean("manuallySelectCoordinates", false);
            progressBarShown = savedInstanceState.getInt("progressBarShown");
            System.out.println("[DEBUG] onRestore, reportsIndex length is " + reportsIndex.length());
        }
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
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        if(manuallySelectCoordinates) {
            // In this behavior, the user can touch the map in order to retrieve a point coordinates
            gmap.setOnMapClickListener(this);
            gmap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    confirmChoice();
                }
            });
        } else {
            if(!fromConfigurationChange) {
                // The application hsa been just started
                showProgressBar(true);
                getReportIndex();
            } else if ((!reportsIndex.isEmpty()) && (!((reportsIndex.equals("0")) && (reportsIndex.equals("-1"))))) {
                // A rotation has occurred. The markers have to be redrawn, if they were already downloaded
                showProgressBar(true);
                parseReportsIndex(reportsIndex);
            }
        }
        //Set the zoom +- keys as visible
        UiSettings settings = gmap.getUiSettings();
        settings.setZoomControlsEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // The user has granted the app the permission to use the GPS. The app now will use it
                        Button button = (Button)findViewById(R.id.button_browseMap_track);
                        if (!gmap.isMyLocationEnabled()) {
                            gmap.setMyLocationEnabled(true);
                            button.setText("Stop Tracking");
                        } else {
                            gmap.setMyLocationEnabled(false);
                            button.setText("Track");
                        }
                    }
                }
                break;
            }
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
    protected static String categoryIndexToString(int categoryIndex){
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

    public void commuteTracking(View v){
        Button button = (Button)findViewById(R.id.button_browseMap_track);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
            } else {
                if (!gmap.isMyLocationEnabled()) {
                    gmap.setMyLocationEnabled(true);
                    button.setText("Stop Tracking");
                } else {
                    gmap.setMyLocationEnabled(false);
                    button.setText("Track");
                }
            }
    }




    public void confirmChoice(){
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

    protected void getCompleteReport(final Marker marker){
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
                            showCompleteReport(marker);
                        }
                        showProgressBar(false);
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

    protected void getReportIndex() {
        System.out.println("[DEBUG] getReportIndex()");
        // TODO qui potrei fare una progress bar che raggiunge il 50% dopo aver scaricato
        // tutto e poi sale gradualmente.
        String url = "http://www.activecitizen.altervista.org/get_report_index/";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // TODO: ricordarsi di gestire in maniera uniforme gli errori, in tutte le activity
                        if(response.equals("0")){
                            System.out.println("[DEBUG] getReportIndex() response is 0");
                            String message = "An error occurred while contacting the server";
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            reportsIndex = response;
                        } else if(response.equals("-1")){
                            System.out.println("[DEBUG] getReportIndex() response is -1");
                            String message = "No report to retrieve";
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            // TODO aggiustare il loading dialog
                            //if(loadingDialog != null && loadingDialog.isShowing()){
                            //    loadingDialog.dismiss();
                            //}
                            reportsIndex = response;
                        } else {
                            try {
                                System.out.println("[DEBUG] getReportIndex() response is complete");
                                JSONObject jObject = new JSONObject("{output:" + response + "}");
                                reportsIndex = jObject.getString("output");
                                parseReportsIndex(reportsIndex);
                            } catch (org.json.JSONException e){
                                String message = "An error occurred while parsing the reports index";
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("[DEBUG] onErrorResponse");
                        String message = "Connection error";
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        reportsIndex = "0";
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

    protected void parseReportsIndex(String reportsIndex) {
        System.out.println("[DEBUG] parseReportsIndex()");
        String[] reports = reportsIndex.split("=");
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
                //System.out.println("[DEBUG] per questo marker, your_rate vale " + newReport.your_rate);
            } else if(newReport.your_rate != 0){
                // The user has already votes this issue
                markerOptions.icon(BitmapDescriptorFactory.
                        defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            }
            System.out.println("[DEBUG] parseReportsIndex(), sto per usare gmap che vale " + gmap);
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
                        showCompleteReport(marker);
                    } else {
                        // Request the full details to the server
                        System.out.println("[DEBUG] I'm going to retrieve the details of " + r.reportTitle);
                        // TODO qui forse dovrei passare anche il marker, così che, se opportuno, venga colorato
                        showProgressBar(true);
                        getCompleteReport(marker);
                    }
                }
            });

            // TODO: aggiornare i valori subito dopo averli modificati
        }
        showProgressBar(false);
    }

    protected void showCompleteReport(Marker marker) {
        reportDialog = ReportDialogFragment.newInstance(marker);
        reportDialog.show(getFragmentManager(), "report_dialog");
    }

    protected void showProgressBar(boolean doIHaveToShow) {
        if(doIHaveToShow) {
            System.out.println("[DEBUG] inside showProgressBar(true)");
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            relativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.uninteractive_screen));
            progressBar_loading.setVisibility(View.VISIBLE);
            progressBarShown = 1;
        } else {
            System.out.println("[DEBUG] inside showProgressBar(false)");
            //progressBar_loading = (ProgressBar) findViewById(R.id.progressBar_browseMap_loading);
            progressBar_loading.setVisibility(View.GONE);
            relativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.background_light));
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            progressBarShown = 0;
        }
    }

    /*
    protected void showReportDialog(final Marker marker) {
        final Report report = (Report) marker.getTag();
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
                final int the_rate_is_new = (report.your_rate == 0 ? 1 : 0);
                System.out.println("[DEBUG]: the_rate_is_new vale " + the_rate_is_new);
                String url = "http://www.activecitizen.altervista.org/rate/";
                //System.out.println("[DEBUG] inside the send rate, rate is " + rating.getRating() +
                //      ", old_rate is " + old_rate);
                StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                if (response.equals("-1")) {
                                    System.out.println("[DEBUG] response is 0 :c");
                                    reportDialog.dismiss();
                                } else {
                                    report.your_rate = rating.getRating();
                                    if (report.authorId != userId) {
                                        // If the report has been issued by the user,
                                        // its color its already HUE_AZURE, no need
                                        // to modifying it
                                        marker.setIcon(BitmapDescriptorFactory.
                                                defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                                        reportDialog.dismiss();
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
                                reportDialog.dismiss();
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("user_id", "" + userId);
                        params.put("report_id", "" + report.reportId);
                        params.put("rate", "" + rating.getRating());
                        params.put("old_rate", "" + report.your_rate);
                        params.put("the_rate_is_new", "" + the_rate_is_new);
                        return params;
                    }
                };
                queue.add(postRequest);
            }
        });
    }
    */

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

}


//android:icon="@android/drawable/..
//TODO: back non sta facendo il suo dovere di, se un marker ha la sua finestrella visualizzata, toglierla e basta
//quando uno clicca per vedere i dettagli di un report, la finestrella relativa al marker relativo dovrebbe scomparire, cosi' che se uno fa indietro non la vede

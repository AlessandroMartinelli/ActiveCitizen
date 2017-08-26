// TODO: forse dovrei aggiungere il fatto che, quando uno fa click sul vuoto, se la tastiera è aperta la si ri-chiude etc.

package com.example.alessandro.activecitizen;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import android.Manifest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.example.alessandro.activecitizen.R.array.categories_array;

/**
 * Created by Alessandro on 04/08/2017.
 */

public class ReportAnIssue extends AppCompatActivity implements LocationListener,
        AdapterView.OnItemSelectedListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_MANUALLY_CHOOSE_COORDINATES = 2;
    static final int LOCATION_PERMISSION = 3;
    static final int PICK_PHOTO = 4;

    // TODO: turn all private into protected
    protected int userId;
    protected Intent startingIntent;

    protected LocationManager locationManager;
    protected Criteria criteria;
    protected String locationProvider;
    protected static String detailsDialogContent; //TODO: needs to be static?
    protected ProgressDialog loadingDialog;

    protected RequestQueue queue;
    protected String url;

    protected LatLng latLng;
    protected Bitmap imageBitmap;
    protected String imageString;
    protected int category;

    protected TextView currentLocation;
    protected EditText reportTitle;
    protected Spinner reportCategory;
    protected EditText reportDetails;
    protected EditText dialogDetails;
    protected ImageView photoPreview;
    protected Button buttonAddPhoto;
    protected RatingBar ratingBar;

    /**
     * Method used for codifying a Bitmap into a String. The String will be
     * stored on a database
     * @param bitmap the Bitmap to be codified
     * @return the string codifying the given Bitmap
     */
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    public String stringFromLatLng(LatLng latLng) {
        String coordinates = new String();
        coordinates = coordinates
                .concat(String.valueOf(latLng.latitude))
                .concat(",\n")
                .concat(String.valueOf(latLng.longitude));
        return coordinates;
    }

    public String stringFromLocation(Location location) {
        String coordinates = new String();
        coordinates = coordinates
                .concat(String.valueOf(location.getLatitude()))
                .concat(",\n")
                .concat(String.valueOf(location.getLongitude()));
        return coordinates;
    }

    public LatLng LatLngFromLocation(Location location){
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    //public void showShortToast(String message){
    //    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    //}

    //public void showLongToast(String message){
    //    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    //}

    protected void initializeLocationProvider(){
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationProvider = locationManager.getBestProvider(criteria, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("[DEBUG] onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_an_issue);

        queue = Volley.newRequestQueue(this);
        url = "http://www.activecitizen.altervista.org/send_report/";

        currentLocation = (TextView) findViewById(R.id.textView_currentLocation);
        reportTitle = (EditText) findViewById(R.id.editText_insertTitle);
        reportCategory = (Spinner) findViewById(R.id.spinner_category);
        reportDetails = (EditText) findViewById(R.id.editText_details);
        photoPreview = (ImageView) findViewById(R.id.imageView_photoPreview);
        buttonAddPhoto = (Button) findViewById(R.id.button_addPhoto);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar_priority);

        CharSequence[] categories = getResources().getStringArray(categories_array);
        ArrayAdapter<CharSequence> arrayAdapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item, categories);
        reportCategory.setAdapter(arrayAdapter);
        reportCategory.setOnItemSelectedListener(this);
        category = 0; // poi occorrerà save instance state e restore

        startingIntent = getIntent();
        userId = startingIntent.getIntExtra("user_id", 0);
        if(userId == 0){
            Toast.makeText(getApplicationContext(), "Account error", Toast.LENGTH_LONG).show();
            finish();
        }
        System.out.println("[DEBUG] onCreate() userId retrieved from intent is " + userId);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        criteria = new Criteria();
        detailsDialogContent = new String("");
    }

    @Override
    protected void onResume() {
        System.out.println("[DEBUG] onResume()");

        super.onResume();
        initializeLocationProvider();
        System.out.println("[DEBUG] locationProvider is this " + locationProvider);
    }

    @Override
    protected void onPause() {
        System.out.println("[DEBUG] onPause()");
        super.onPause();
        locationManager.removeUpdates(this); // forse andrebbe in onStop?
    }

    @Override
    protected void onStop(){
        System.out.println("[DEBUG] onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        System.out.println("[DEBUG] onDestroy()");
        super.onDestroy();
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("bitmap", imageBitmap);
        outState.putParcelable("coordinates", latLng);
        outState.putInt("user_id", userId);
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        System.out.println("[DEBUG] after default onRestore(), userId is " + userId);

        imageBitmap = savedInstanceState.getParcelable("bitmap");
        if(imageBitmap != null) {
            photoPreview.setImageBitmap(imageBitmap);
        } else {
            photoPreview.setImageResource(android.R.drawable.gallery_thumb);
        }

        latLng = savedInstanceState.getParcelable("coordinates");
        if(latLng != null) {
            currentLocation.setText(stringFromLatLng(latLng));
        }

        userId = savedInstanceState.getInt("user_id", 0) != 0?
                savedInstanceState.getInt("user_id", 0) : userId;
        System.out.println("[DEBUG] after complete onRestore(), userId is " + userId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        initializeLocationProvider();
                    }
                }
                break;
            }
        }
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Result returned from the capture photo app
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            photoPreview.setImageBitmap(imageBitmap);
            buttonAddPhoto.setText("change photo");
            imageString = BitMapToString(imageBitmap);
            // TODO: la parte sottostante è solo di debug, andrà tolta
            System.out.println("[DEBUG] sent imageString length is " + imageString.length());
            Bitmap imageBitmapNew = StringToBitMap(imageString);
            photoPreview.setImageBitmap(imageBitmapNew);
        } else if (requestCode == PICK_PHOTO && resultCode == Activity.RESULT_OK) {
            // Result returned from the pick photo from gallery app
            if (data == null) {
                String message = "An error occurred while retrieving the image";
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                // The image is retrieved
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                imageBitmap = BitmapFactory.decodeStream(inputStream);

                // The image is compressed
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStream);

                // The imageString String, to be used in the send, is initialized
                byte [] b=outputStream.toByteArray();
                imageString= Base64.encodeToString(b, Base64.DEFAULT);
                System.out.println("[DEBUG] selected image imageString length is " + imageString.length());

                // The compressed image is retrieved
                byte[] bitmapdata = outputStream.toByteArray();
                imageBitmap = BitmapFactory.decodeByteArray(bitmapdata, 0, bitmapdata.length);

                photoPreview.setImageBitmap(imageBitmap);
                buttonAddPhoto.setText("change photo");
            } catch(Exception e){
                String message = "An error occurred while retrieving the image";
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                System.out.println("[DEBUG] exception: " + e.getMessage());
            }
        } else if(requestCode == REQUEST_MANUALLY_CHOOSE_COORDINATES && resultCode == RESULT_OK) {
            // Result returned from the application used for let the user choose coordinates
            locationManager.removeUpdates(this); // forse andrebbe in onStop?
            Bundle extras = data.getExtras();
            latLng = (LatLng) extras.get("coordinates");
            currentLocation.setText(stringFromLatLng(latLng));
        }
    }

    public void obtainCurrentCoordinates(View view) {
        if (locationProvider == null) {
            currentLocation.setText("unable to detect your location");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
            }
        } else {
            locationManager.requestSingleUpdate(locationProvider, this, null);
            currentLocation.setText("...detecting...");
            latLng = null;
        }
    }

    @Override
    public void onLocationChanged(Location location){
        currentLocation.setText(stringFromLocation(location));
        latLng = LatLngFromLocation(location);
    }

    protected void manuallySelectCoordinates(View v) {
        // TODO: questa cosa dovrei metterla ogni volta?
        // secondo me no, secondo me giusto se chiamo roba esterna, ma non in questo caso
        Intent manualCoordinatesIntent = new Intent(this, BrowseMap.class);
        if(manualCoordinatesIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(manualCoordinatesIntent, REQUEST_MANUALLY_CHOOSE_COORDINATES);
        } else {
            String message = "No such application is available on your device";
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int index, long id){
        // TODO: category posso rimuoverlo da questa activity
        category = index;
    }

    public void showDetailsDialog(View v) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        final View inflater = layoutInflater.inflate(R.layout.dialog_insert_details, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(inflater);

        dialogDetails = (EditText) inflater.findViewById(R.id.editText_dialog_details);
        dialogDetails.setText(detailsDialogContent);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                System.out.println("[DEBUG] positive button pressed");
                reportDetails.setText(dialogDetails.getText());
                detailsDialogContent = dialogDetails.getText().toString();
            }
        })
                .setNegativeButton("Do Not Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        System.out.println("[DEBUG] negative button pressed");
                        dialog.cancel();
                    }
                })
                .setCancelable(false)
                .show();
    }

    public void pickImageDialog(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Choose an image from")
                .setItems(R.array.image_choice_array, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        System.out.println("[DEBUG] choice is " + which);
                        switch (which) {
                            case 0:
                                // Camera has been chosen
                                takeAPhoto(null);
                                break;
                            // Gallery has been chosen
                            case 1:
                                pickImage(null);
                                break;
                            default:
                                break;
                        }
                    }
                });
        builder.show();
    }

    protected void takeAPhoto(View v){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            String message = "No such application is available on your device";
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    public void pickImage(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO);
    }

    public void resetReport(View v){
        currentLocation.setText(R.string.coordinates_retrieval_instruction);
        reportTitle.setText("");
        reportCategory.setSelection(0);
        category = 0;
        reportDetails.setText("");
        photoPreview.setImageResource(android.R.drawable.gallery_thumb);
        imageBitmap = null;
        latLng = null;
        buttonAddPhoto.setText("take a photo");
        ratingBar.setRating(0);
    }

    public void sendReport(View v) {
        System.out.println("[DEBUG] title is " + reportTitle.getText().toString());
        // per prima cosa si dovrà controllare che tutte le variabili non siano nulle
        // poi la conversione dell'immagine a stringa la farei qui, così da tenere la send
        // quanto più possibile leggera.
        // Dopo aver chiamato la login bisogna anche mostrare il dialogo di caricamento.
        if(latLng == null){
            Toast.makeText(getApplicationContext(), "You must insert the coordinates", Toast.LENGTH_LONG).show();
            return;
        } else if (reportTitle.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "You must insert a title", Toast.LENGTH_LONG).show();
            return;
        } else if (category == 0){
            Toast.makeText(getApplicationContext(), "You must choose a category", Toast.LENGTH_LONG).show();
            return;
        } else if (reportDetails.getText().toString().isEmpty()){
            Toast.makeText(getApplicationContext(), "You must insert a description", Toast.LENGTH_LONG).show();
            return;
        } else if (imageBitmap == null){
            Toast.makeText(getApplicationContext(), "You must pick an image", Toast.LENGTH_LONG).show();
            return;
        } else if (ratingBar.getRating() == 0){
            Toast.makeText(getApplicationContext(), "You must rate the issue (0 is not accepted)", Toast.LENGTH_LONG).show();
            return;
        }

        loadingDialog = ProgressDialog.show(this, "", "Please wait...", true);

        String userIdString = "" + userId;
        String lat = "" + latLng.latitude;
        String lng = "" + latLng.longitude;
        String ratingString = "" + ratingBar.getRating();
        String categoryString = "" + category;
        //String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());


        send(userIdString, lat, lng, reportTitle.getText().toString(), categoryString,
                reportDetails.getText().toString(), imageString, ratingString);




        /*
         *  TODO: per evitare ri-utilizzi della stessa immagine,
         *  sarebbe buono, alla fine di questo metodo, salvare da qualche
         *  parte l'immagine e mettere a null la variabile globale che
         *  memorizza l'immagine.
         *
         */
    }

    private int send(final String userIdString, final String lat, final String lng,
                     final String title, final String categoryString,
                     final String details, final String imageString, final String ratingString) {
        //System.out.println("[DEBUG] inside the send, variables are: userIdString=" + userIdString + ", lat=" + lat + "" +
          //      ", lng=" + lng + ", title=" + title + ", categoryString=" + categoryString + ", date=" + date + "" +
            //    ", details=" + details + ", imageString=" + imageString + ", ratingString=" + ratingString);
            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // response
                            System.out.println("[DEBUG] Response" + response);
                            if(response.equals("0")){
                                String message = "An error occurred while sending the report";
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                if(loadingDialog != null && loadingDialog.isShowing()){
                                    loadingDialog.dismiss();
                                }
                            } else {
                                String message = "Report sent successfully";
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                if(loadingDialog != null && loadingDialog.isShowing()){
                                    loadingDialog.dismiss();
                                }
                                finish();
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
                    params.put("author_id", userIdString);
                    params.put("title", title);
                    params.put("category", categoryString);
                    params.put("details", details);
                    params.put("latitude", lat);
                    params.put("longitude", lng);
                    params.put("image", imageString);
                    params.put("rating", ratingString);
                    return params;
                }
            };
            queue.add(postRequest);
            return 0;
        }

    /*
    @Override
    public void onBackPressed(){
        if(backButtonPressed == 0){
            showLongToast("Press back again to exit. Inserted data will be lost");
            backButtonPressed = 1;
        } else {
            // Reset the backButtonPressed flag
            backButtonPressed = 0;
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
    @Override
    public void onNothingSelected(AdapterView<?> arg0){}

}

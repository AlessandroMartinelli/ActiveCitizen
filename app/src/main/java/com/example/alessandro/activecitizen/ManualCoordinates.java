package com.example.alessandro.activecitizen;

import android.app.Activity;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static java.util.concurrent.ThreadLocalRandom.current;

/**
 * Created by Alessandro on 06/08/2017.
 */

public class ManualCoordinates extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, LocationListener {

    private LocationManager locationManager;
    private GoogleMap gmap;
    private LatLng manuallySelectedCoordinates;
    private LatLng currentCoordinates;
    private String provider;

    public void showToast(String message){
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_coordinates);

        ((MapFragment) getFragmentManager().findFragmentById(R.id.coordinates_map)).
                getMapAsync(this);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);
        if(provider.isEmpty()){
            System.out.println("[DEBUG] nessun provider trovato");
        } else {
            System.out.println("[DEBUG]  provider trovato: " + provider);
            locationManager.requestSingleUpdate(provider, this, null);
            System.out.println("[DEBUG] richiesta al " + " fatta");
        }

        System.out.println("[DEBUG]: ho appena chiesto la mappa");

    }

    @Override
    public void onLocationChanged(Location location){
        System.out.println("[DEBUG] onLocationChanged inizia qua");
        //currentCoordinates.latitude = location.getLatitude();
        currentCoordinates = new LatLng (location.getLatitude(), location.getLongitude());
        if(gmap != null) {
            gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, 15));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        System.out.println("[DEBUG]: ecco la mappa!");
        gmap = googleMap;
        gmap.setOnMapClickListener(this);
        centerMap(null);
        showToast("Wait a few seconds for the map to be centered to your location...");
    }

    @Override
    public void onMapClick(LatLng clickedCoordinates){
        manuallySelectedCoordinates = clickedCoordinates;

        gmap.clear();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(manuallySelectedCoordinates);
        gmap.addMarker(markerOptions);

        String coordinates = new String();
        coordinates = coordinates
                .concat(String.valueOf(manuallySelectedCoordinates.latitude))
                .concat(", ")
                .concat(String.valueOf(manuallySelectedCoordinates.longitude));
        System.out.println("[DEBUG] coordinate:" + coordinates);
    }

    /*
     * If the current coordinates are available, the camera is centered
     * in that location; then, indipendently from the current position availabiltiy,
     * the current position is requested again (it will presumably be used
     * next time "center map" button will be pressed).
     */
    public void centerMap(View v){
        locationManager.requestSingleUpdate(provider, this, null);
    }

    public void ok(View v){
        if(manuallySelectedCoordinates == null){
            // toast che dice che non hai selezionato alcun punto sulla mappa
            // e che puoi premere indietro per tornare all'attivita' precedente
        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("coordinates", manuallySelectedCoordinates);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
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


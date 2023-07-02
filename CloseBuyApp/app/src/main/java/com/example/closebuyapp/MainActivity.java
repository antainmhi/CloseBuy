package com.example.closebuyapp;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.Place;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationClient;
    private EditText userInput;
    private Spinner sortOptionSpinner;
    private Location lastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Places SDK
        Places.initialize(getApplicationContext(), "AIzaSyD2ZitIcHRLLvZWBueCb8AV5c_YsnLam7M");

        // Create a new Places client instance
        placesClient = Places.createClient(this);

        // Reference to the EditText and Spinner in the layout
        userInput = findViewById(R.id.userInput);
        sortOptionSpinner = findViewById(R.id.sortOptionSpinner);

        // Get the FusedLocationProviderClient for accessing location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If not granted, request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            // If granted, get the last known location
            getLastKnownLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // This method is called when the user responds to the permission request
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If the permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Get the last known location
                getLastKnownLocation();
            } else {
                // If the permission is denied, inform the user with a toast
                Toast.makeText(this, "Permission denied. Unable to access location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLastKnownLocation() {
        // Get the last known location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // If the location is not null, save it
                    if (location != null) {
                        lastKnownLocation = location;
                    }
                })
                .addOnFailureListener(e -> {
                    // If there is an error getting the location, inform the user with a toast
                    Toast.makeText(MainActivity.this, "Error trying to get last GPS location", Toast.LENGTH_SHORT).show();
                });
    }

    public void searchPlaces(View view) {
        // Get the user's search term and sort option
        String searchTerm = userInput.getText().toString();
        String sortOption = sortOptionSpinner.getSelectedItem().toString();

        // Specify the fields to return
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.RATING, Place.Field.LAT_LNG);

        // Construct a request object, passing the place ID and fields array
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);

        // Make the request
        placesClient.findCurrentPlace(request)
                .addOnSuccessListener(response -> {
                    ArrayList<Place> placeList = new ArrayList<>();

                    // For each place, add it to the list if its name contains the user's search term
                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        if (placeLikelihood.getPlace().getName().toLowerCase().contains(searchTerm.toLowerCase())) {
                            placeList.add(placeLikelihood.getPlace());
                        }
                    }

                    // Sort the places based on the user's selected sort option
                    if (sortOption.equals("Distance")) {
                        Collections.sort(placeList, new Comparator<Place>() {
                            @Override
                            public int compare(Place p1, Place p2) {
                                float[] results1 = new float[1];
                                Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                                        p1.getLatLng().latitude, p1.getLatLng().longitude, results1);

                                float[] results2 = new float[1];
                                Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                                        p2.getLatLng().latitude, p2.getLatLng().longitude, results2);

                                return Float.compare(results1[0], results2[0]);
                            }
                        });
                    } else if (sortOption.equals("Rating")) {
                        Collections.sort(placeList, new Comparator<Place>() {
                            @Override
                            public int compare(Place p1, Place p2) {
                                Float rating1 = p1.getRating() != null ? p1.getRating() : 0;
                                Float rating2 = p2.getRating() != null ? p2.getRating() : 0;

                                return rating2.compareTo(rating1);
                            }
                        });
                    }

                    // Display the sorted places
                    for (Place place : placeList) {
                        String rating = place.getRating() != null ? String.valueOf(place.getRating()) : "No rating";
                        float[] results = new float[1];
                        Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                                place.getLatLng().latitude, place.getLatLng().longitude, results);
                        float distanceInKm = results[0] / 1000;
                        Toast.makeText(MainActivity.this, place.getName() + ", " + place.getAddress() + ", Rating: " + rating + ", Distance: " + distanceInKm + " km", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // If there is an error making the request, inform the user with a toast
                    Toast.makeText(MainActivity.this, "Error contacting API: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

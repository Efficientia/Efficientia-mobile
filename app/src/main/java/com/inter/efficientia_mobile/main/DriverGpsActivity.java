package com.inter.efficientia_mobile.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.PlaceAutocomplete;
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity;
import com.inter.efficientia_mobile.BuildConfig;
import com.inter.efficientia_mobile.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Optional driver map: never changes route diary data or trip state. */
public class DriverGpsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String STATE_LAT = "selected_lat";
    private static final String STATE_LNG = "selected_lng";
    private static final String STATE_NAME = "selected_name";
    private static final String STATE_ADDRESS = "selected_address";
    private static final String STATE_ATTRIBUTIONS = "selected_attributions";
    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasLocationPermission()) centerOnCurrentLocation();
                else showStatus(R.string.gps_permission_denied);
            });
    private final ActivityResultLauncher<Intent> placeSearch = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data == null) return;
                if (result.getResultCode() == PlaceAutocompleteActivity.RESULT_OK) {
                    AutocompletePrediction prediction = PlaceAutocomplete.getPredictionFromIntent(data);
                    AutocompleteSessionToken token = PlaceAutocomplete.getSessionTokenFromIntent(data);
                    if (prediction != null) fetchPlace(prediction, token);
                } else if (result.getResultCode() == PlaceAutocompleteActivity.RESULT_ERROR) {
                    showStatus(R.string.gps_search_failed);
                }
            });

    private GoogleMap map;
    private PlacesClient placesClient;
    private Marker selectedMarker;
    private LatLng selectedLocation;
    private String selectedName;
    private String selectedAddress;
    private ArrayList<String> selectedAttributions;
    private Bundle restoredSelection;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_driver_gps);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gpsRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        status = findViewById(R.id.txtGpsStatus);
        restoredSelection = savedInstanceState;
        findViewById(R.id.btnGpsBack).setOnClickListener(view -> ToolbarNavigation.back(this));
        findViewById(R.id.gpsSearchBar).setOnClickListener(view -> searchForPlace());
        findViewById(R.id.btnGpsMyLocation).setOnClickListener(view -> requestCurrentLocation());
        findViewById(R.id.btnGpsDirections).setOnClickListener(view -> openDirections());

        if (BuildConfig.MAPS_API_KEY.trim().isEmpty()) {
            showStatus(R.string.gps_key_missing);
            return;
        }
        Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        placesClient = Places.createClient(this);
        SupportMapFragment fragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.gpsMapContainer, fragment).commitNow();
        fragment.getMapAsync(this);
    }

    @Override public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setPadding(0, dp(100), 0, dp(175));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-14.235, -51.9253), 4f));
        map.setOnMapClickListener(point -> showSelectedPlace(point,
                getString(R.string.gps_selected_point), "", null));
        if (restoredSelection != null && restoredSelection.containsKey(STATE_LAT)) {
            showSelectedPlace(new LatLng(restoredSelection.getDouble(STATE_LAT),
                    restoredSelection.getDouble(STATE_LNG)),
                    restoredSelection.getString(STATE_NAME),
                    restoredSelection.getString(STATE_ADDRESS),
                    restoredSelection.getStringArrayList(STATE_ATTRIBUTIONS));
        } else {
            requestCurrentLocation();
        }
    }

    private void searchForPlace() {
        if (placesClient == null) {
            showStatus(R.string.gps_key_missing);
            return;
        }
        placeSearch.launch(new PlaceAutocomplete.IntentBuilder().build(this));
    }

    private void fetchPlace(AutocompletePrediction prediction, AutocompleteSessionToken token) {
        showStatus(R.string.gps_searching);
        FetchPlaceRequest.Builder builder = FetchPlaceRequest.builder(prediction.getPlaceId(),
                Arrays.asList(Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS,
                        Place.Field.LOCATION));
        if (token != null) builder.setSessionToken(token);
        placesClient.fetchPlace(builder.build()).addOnSuccessListener(this, response -> {
            Place place = response.getPlace();
            if (place.getLocation() == null) {
                showStatus(R.string.gps_search_failed);
                return;
            }
            showSelectedPlace(place.getLocation(), place.getDisplayName(),
                    place.getFormattedAddress(), place.getAttributions());
        }).addOnFailureListener(this, error -> showStatus(R.string.gps_search_failed));
    }

    private void showSelectedPlace(LatLng point, String name, String address,
                                   List<String> attributions) {
        selectedLocation = point;
        if (selectedMarker != null) selectedMarker.remove();
        String title = name == null || name.trim().isEmpty() ? getString(R.string.gps_selected_point) : name;
        selectedName = title;
        selectedAddress = address;
        selectedAttributions = attributions == null ? null : new ArrayList<>(attributions);
        ((TextView) findViewById(R.id.btnGpsSearch)).setText(title);
        selectedMarker = map.addMarker(new MarkerOptions().position(point).title(title));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));
        ((TextView) findViewById(R.id.txtGpsPlaceName)).setText(title);
        ((TextView) findViewById(R.id.txtGpsPlaceAddress)).setText(address == null ? "" : address);
        TextView attributionView = findViewById(R.id.txtGpsAttributions);
        if (attributions == null || attributions.isEmpty()) {
            attributionView.setVisibility(View.GONE);
        } else {
            attributionView.setText(Html.fromHtml(String.join(" · ", attributions),
                    Html.FROM_HTML_MODE_COMPACT));
            attributionView.setMovementMethod(LinkMovementMethod.getInstance());
            attributionView.setVisibility(View.VISIBLE);
        }
        findViewById(R.id.gpsPlaceCard).setVisibility(View.VISIBLE);
        status.setVisibility(View.GONE);
    }

    private void requestCurrentLocation() {
        if (map == null) return;
        if (!hasLocationPermission()) {
            locationPermission.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        centerOnCurrentLocation();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void centerOnCurrentLocation() {
        if (map == null || !hasLocationPermission()) return;
        showStatus(R.string.gps_locating);
        map.setMyLocationEnabled(true);
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,
                        new CancellationTokenSource().getToken())
                .addOnSuccessListener(this, location -> {
                    if (location == null) {
                        showStatus(R.string.gps_location_unavailable);
                    } else {
                        float zoom = ContextCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                ? 17f : 12f;
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), zoom));
                        status.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(this, error -> showStatus(R.string.gps_location_unavailable));
    }

    private void openDirections() {
        if (selectedLocation == null) return;
        String coordinates = selectedLocation.latitude + "," + selectedLocation.longitude;
        Intent nativeMaps = new Intent(Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=" + coordinates + "&mode=d"));
        nativeMaps.setPackage("com.google.android.apps.maps");
        try {
            if (nativeMaps.resolveActivity(getPackageManager()) != null) {
                startActivity(nativeMaps);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&destination="
                                + coordinates + "&travelmode=driving")));
            }
            String destination = getString(R.string.gps_selected_point).equals(selectedName)
                    ? coordinates : selectedName;
            DriverPreferences.setGpsLastDestination(this, destination);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.gps_directions_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void showStatus(int message) {
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (selectedLocation != null) {
            outState.putDouble(STATE_LAT, selectedLocation.latitude);
            outState.putDouble(STATE_LNG, selectedLocation.longitude);
            outState.putString(STATE_NAME, selectedName);
            outState.putString(STATE_ADDRESS, selectedAddress);
            outState.putStringArrayList(STATE_ATTRIBUTIONS, selectedAttributions);
        }
        super.onSaveInstanceState(outState);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}

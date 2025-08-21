package com.googleplacessdk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GooglePlacesSdk Module for React Native
 * Works in both Old Architecture and New Architecture
 */
public class GooglePlacesSdkModule extends ReactContextBaseJavaModule {

    public static final String NAME = "GooglePlacesSdk";
    private final ReactApplicationContext reactContext;
    private PlacesClient placesClient;

    public GooglePlacesSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void initialize(String apiKey, Promise promise) {
        try {
            if (!Places.isInitialized()) {
                Places.initialize(reactContext.getApplicationContext(), apiKey);
            }
            placesClient = Places.createClient(reactContext);
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("INIT_ERROR", "Error initializing Google Places SDK", e);
        }
    }

    @ReactMethod
    public void autoComplete(String query, ReadableMap options, Promise promise) {
        WritableMap response = Arguments.createMap();
        if (query == null || query.isEmpty()) {
            response.putInt("status", 400);
            response.putString("message", "Missing query parameter");
            promise.resolve(response);
            return;
        }
        // Create autocomplete request builder
        FindAutocompletePredictionsRequest.Builder requestBuilder = FindAutocompletePredictionsRequest.builder()
                .setQuery(query);
        if (options.hasKey("region_code")) {
            String regionCode = options.getString("region_code");
            requestBuilder.setRegionCode(regionCode);
        }

        if (options.hasKey("location") && options.hasKey("radius")) {
            // If location and radius are provided, create a circular bounds
            ReadableMap location = options.getMap("location");
            double latitude = location.getDouble("latitude");
            double longitude = location.getDouble("longitude");
            double radius = options.getDouble("radius");
            // LatLng center = new LatLng(latitude, longitude);
            double latDelta = radius / 111000.0; // 1 degree latitude ≈ 111km
            double lngDelta = radius / (111000.0 * Math.cos(Math.toRadians(latitude)));
            LatLng southwest = new LatLng(latitude - latDelta, longitude - lngDelta);
            LatLng northeast = new LatLng(latitude + latDelta, longitude + lngDelta);
            RectangularBounds bounds = RectangularBounds.newInstance(southwest, northeast);
            requestBuilder.setLocationBias(bounds);
        }


        // Add countries filter if specified
        if (options.hasKey("countries")) {
            ReadableArray countriesArray = options.getArray("countries");
            List<String> countries = new ArrayList<>();
            if (countriesArray != null && countriesArray.size() > 0) {
                for (int i = 0; i < countriesArray.size(); i++) {
                    countries.add(countriesArray.getString(i));
                }
                requestBuilder.setCountries(countries);
            }
        }

        // Add type filter if specified
        if (options.hasKey("types")) {
            ReadableArray typesArray = options.getArray("types");
            List<String> typeFilters = new ArrayList<>();
            if (typesArray != null && typesArray.size() > 0) {
                for (int i = 0; i < typesArray.size(); i++) {
                    String typeFilterString = typesArray.getString(i);
                    String typeFilter = getTypeFilter(typeFilterString);
                    if (typeFilter != null) {
                        typeFilters.add(typeFilter);
                    }
                }
            }
            if (!typeFilters.isEmpty()) {
                requestBuilder.setTypesFilter(typeFilters);
            }
        }

        // Create and send the request
        FindAutocompletePredictionsRequest request = requestBuilder.build();
        placesClient.findAutocompletePredictions(request).addOnSuccessListener((result) -> {
            WritableArray predictions = Arguments.createArray();
            for (AutocompletePrediction prediction : result.getAutocompletePredictions()) {
                WritableMap predictionMap = Arguments.createMap();
                predictionMap.putString("place_id", prediction.getPlaceId());
                predictionMap.putString("description", prediction.getFullText(null).toString());
                predictionMap.putArray("types", Arguments.fromList(prediction.getTypes()));
                predictions.pushMap(predictionMap);
            }

            response.putArray("result", predictions);
            response.putInt("status", 200);
            promise.resolve(response);

        }).addOnFailureListener((exception) -> {
            response.putInt("status", 400);
            response.putString("message", exception.getMessage());
            promise.resolve(response);
        });
    }

    @ReactMethod
    public void fetchPlace(String placeId, ReadableArray fields, Promise promise) {
        WritableMap response = Arguments.createMap();
        if (placeId == null || placeId.isEmpty()) {
            response.putInt("status", 400);
            response.putString("message", "Missing placeId parameter");
            promise.resolve(response);
            return;
        }

        List<Place.Field> placeFields = new ArrayList<>();

        for (int i = 0; i < fields.size(); i++) {
            String field = fields.getString(i);
            Place.Field placeField = getPlaceField(field);
            if (placeField != null) {
                placeFields.add(placeField);
            }
        }

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
        placesClient.fetchPlace(request).addOnSuccessListener((result) -> {
            Place place = result.getPlace();
            WritableMap placeMap = Arguments.createMap();

            for (Place.Field field : placeFields) {
                placeToMap(placeMap, place, field);
            }

            response.putInt("status", 200);
            response.putMap("result", placeMap);
            promise.resolve(response);
        }).addOnFailureListener((exception) -> {
            response.putInt("status", 400);
            response.putString("message", exception.getMessage());
            promise.resolve(response);
        });
    }

    // Helper methods required to make the module work properly
    private String getTypeFilter(String typeFilterString) {
        switch (typeFilterString) {
            case "address":
                return PlaceTypes.ADDRESS;
            case "geocode":
                return PlaceTypes.GEOCODE;
            case "establishment":
                return PlaceTypes.ESTABLISHMENT;
            case "regions":
                return PlaceTypes.REGIONS;
            case "cities":
                return PlaceTypes.CITIES;
            default:
                return null;
        }
    }

    private Place.Field getPlaceField(String fieldString) {
        switch (fieldString) {
            case "name":
                return Place.Field.NAME;
            case "place_id":
                return Place.Field.ID;
            case "address":
                return Place.Field.ADDRESS;
            case "phone_number":
                return Place.Field.PHONE_NUMBER;
            case "types":
                return Place.Field.TYPES;
            case "website":
                return Place.Field.WEBSITE_URI;
            case "coordinate":
            case "location":
                return Place.Field.LAT_LNG;
            case "rating":
                return Place.Field.RATING;
            case "price_level":
                return Place.Field.PRICE_LEVEL;
            case "user_ratings_total":
                return Place.Field.USER_RATINGS_TOTAL;
            case "address_components":
                return Place.Field.ADDRESS_COMPONENTS;
            case "opening_hours":
                return Place.Field.OPENING_HOURS;
            // Add other fields as needed
            default:
                return null;
        }
    }

    private void placeToMap(WritableMap placeMap, Place place, Place.Field field) {
        try {
            switch (field) {
                case NAME:
                    if (place.getName() != null) {
                        placeMap.putString("name", place.getName());
                    }
                    break;
                case ID:
                    if (place.getId() != null) {
                        placeMap.putString("place_id", place.getId());
                    }
                    break;
                case ADDRESS:
                    if (place.getAddress() != null) {
                        placeMap.putString("formatted_address", place.getAddress());
                    }
                    break;
                case PHONE_NUMBER:
                    if (place.getPhoneNumber() != null) {
                        placeMap.putString("phone_number", place.getPhoneNumber());
                    }
                    break;
                case WEBSITE_URI:
                    if (place.getWebsiteUri() != null) {
                        placeMap.putString("website", place.getWebsiteUri().toString());
                    }
                    break;
                case LAT_LNG:
                    if (place.getLatLng() != null) {
                        WritableMap locationMap = Arguments.createMap();
                        locationMap.putDouble("lat", place.getLatLng().latitude);
                        locationMap.putDouble("lng", place.getLatLng().longitude);
                        WritableMap geometryMap = Arguments.createMap();
                        geometryMap.putMap("location", locationMap);
                        placeMap.putMap("geometry", geometryMap);
                    }
                    break;
                case RATING:
                    if (place.getRating() != null) {
                        placeMap.putDouble("rating", place.getRating());
                    }
                    break;
                case PRICE_LEVEL:
                    if (place.getPriceLevel() != null) {
                        placeMap.putInt("price_level", place.getPriceLevel());
                    }
                    break;
                case TYPES:
                    if (place.getTypes() != null) {
                        WritableArray typesArray = Arguments.createArray();
                        for (Place.Type type : place.getTypes()) {
                            typesArray.pushString(type.name());
                        }
                        placeMap.putArray("types", typesArray);
                    }
                    break;
                case USER_RATINGS_TOTAL:
                    if (place.getUserRatingsTotal() != null) {
                        placeMap.putInt("user_ratings_total", place.getUserRatingsTotal());
                    }
                    break;
                // Handle other fields as needed
                default:
                    break;
            }
        } catch (Exception e) {
            // Skip this field if there's an issue accessing it
        }
    }

    @ReactMethod
    public void getAPIKey(Promise promise) {
        try {
            Context context = reactContext.getApplicationContext();
            String apiKey = context.getResources().getString(
                    context.getResources().getIdentifier("GoogleMapsApiKey", "string", context.getPackageName())
            );
            promise.resolve(apiKey);
        } catch (Exception e) {
            promise.resolve(null);
        }
    }
}

package com.example.qride.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TramXeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private final List<MainActivity.BikeStation> stationList = new ArrayList<>(); // Thêm lại
    private OnVehicleLoadListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tram_xe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
//        initSampleStations(); // Thêm lại

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapFragmentContainer);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapFragmentContainer, mapFragment)
                    .commitNow();
        }

        mapFragment.getMapAsync(this);
    }

//    // Thêm lại hàm này
//    private void initSampleStations() {
//        stationList.clear();
//        stationList.add(new MainActivity.BikeStation(
//                "Trạm Bến Thành", "Quận 1, TP.HCM", 8,
//                new LatLng(10.7726, 106.6980)));
//        stationList.add(new MainActivity.BikeStation(
//                "Trạm Lê Thánh Tôn", "26 Lê Thánh Tôn, Q.1", 5,
//                new LatLng(10.7742, 106.6963)));
//        stationList.add(new MainActivity.BikeStation(
//                "Trạm Hàm Nghi", "Hàm Nghi, Q.1", 10,
//                new LatLng(10.7710, 106.7030)));
//    }

    // ======================== Goi API tram xe ====================
    private void loadStationsFromAPI() {
        String url = APIHelper.BASE_URL + "stations";

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    stationList.clear();

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            int id = obj.getInt("id");
                            String name = obj.getString("name");
                            String address = obj.getString("address");
                            double lat = obj.getDouble("lat");
                            double lng = obj.getDouble("lng");
                            MainActivity.BikeStation station =
                                    new MainActivity.BikeStation(
                                            id,
                                            name,
                                            address,
                                            0,
                                            new LatLng(lat, lng)
                                    );
                            stationList.add(station);
                        }
                        showMarkers();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    error.printStackTrace();
                }
        );
        queue.add(request);
    }

    // ================ Hien thi Marker ===============
    private void showMarkers() {
        if (mMap == null) return;

        mMap.clear();

        for (MainActivity.BikeStation station : stationList) {
            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(station.location)
                            .title(station.name)
            );
            if (marker != null) marker.setTag(station);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        loadStationsFromAPI();
        mMap.setOnMarkerClickListener(marker -> {
            MainActivity.BikeStation station = (MainActivity.BikeStation) marker.getTag();

            if (station != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showStationCard(
                        station.name,
                        station.address
                );
                loadVehiclesByStation(station.id);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(station.location));
            }
            return true;
        });

        // Click bản đồ → ẩn card
        mMap.setOnMapClickListener(latLng -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).hideStationCard();
            }
        });

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(10.7710, 106.7000), 15f));
    }

    public void locateMe() {
        if (mMap == null) return;
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                }
            });
        }
    }

    public void searchStation(String query) {
        if (mMap == null) return;
        for (MainActivity.BikeStation s : stationList) { // Thêm lại logic search
            if (s.name.toLowerCase().contains(query.toLowerCase())) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(s.location, 17f));
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showStationCard(s.name, s.address);
                }
                break;
            }
        }
    }

    private void loadVehiclesByStation(int stationId) {
        String url = APIHelper.BASE_URL + "vehicles/" + stationId;
        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (listener != null) {
                        listener.onVehiclesLoaded(response);
                    }
                },
                error -> {
                    error.printStackTrace();
                }
        );
        queue.add(request);
    }

    public interface OnVehicleLoadListener {
        void onVehiclesLoaded(JSONArray vehicles);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnVehicleLoadListener) {
            listener = (OnVehicleLoadListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
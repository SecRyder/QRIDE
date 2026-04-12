package com.example.qride.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.qride.MainActivity;
import com.example.qride.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class TramXeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private final List<MainActivity.BikeStation> stationList = new ArrayList<>(); // ✅ Thêm lại

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
        initSampleStations(); // ✅ Thêm lại

        // ✅ Tạo map fragment bằng code, tránh conflict với ViewPager
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

    // ✅ Thêm lại hàm này
    private void initSampleStations() {
        stationList.clear();
        stationList.add(new MainActivity.BikeStation(
                "Trạm Bến Thành", "Quận 1, TP.HCM", 8,
                new LatLng(10.7726, 106.6980)));
        stationList.add(new MainActivity.BikeStation(
                "Trạm Lê Thánh Tôn", "26 Lê Thánh Tôn, Q.1", 5,
                new LatLng(10.7742, 106.6963)));
        stationList.add(new MainActivity.BikeStation(
                "Trạm Hàm Nghi", "Hàm Nghi, Q.1", 10,
                new LatLng(10.7710, 106.7030)));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // ✅ Thêm lại toàn bộ marker
        for (MainActivity.BikeStation station : stationList) {
            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(station.location)
                            .title(station.name));
            if (marker != null) marker.setTag(station);
        }

        // ✅ Thêm lại click marker → hiện card
        mMap.setOnMarkerClickListener(marker -> {
            MainActivity.BikeStation station = (MainActivity.BikeStation) marker.getTag();
            if (station != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showStationCard(station.name, station.address);
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
        for (MainActivity.BikeStation s : stationList) { // ✅ Thêm lại logic search
            if (s.name.toLowerCase().contains(query.toLowerCase())) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(s.location, 17f));
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showStationCard(s.name, s.address);
                }
                break;
            }
        }
    }
}
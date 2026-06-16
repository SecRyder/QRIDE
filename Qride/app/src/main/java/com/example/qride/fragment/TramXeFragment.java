package com.example.qride.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.adapter.VehicleAdapter;
import com.example.qride.helper.APIHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TramXeFragment extends Fragment implements OnMapReadyCallback {

    private long nextBookingTime = 0; // Lưu thời gian (miliseconds) được phép đặt lại
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private final List<MainActivity.BikeStation> stationList = new ArrayList<>(); // Thêm lại
    private OnVehicleLoadListener listener;
    private com.google.android.gms.maps.model.Polyline currentRoute;
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
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bike_logo))
            );
            if (marker != null) marker.setTag(station);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        loadStationsFromAPI();

        // Sửa sự kiện click vào Marker tại đây
        mMap.setOnMarkerClickListener(marker -> {
            MainActivity.BikeStation station = (MainActivity.BikeStation) marker.getTag();

            if (station != null) {
                // 1. Gọi hàm hiện BottomSheetDialog thay vì hiện CardView của MainActivity
                showStationDetailDialog(station);

                // 2. Di chuyển camera đến trạm để người dùng dễ quan sát
                mMap.animateCamera(CameraUpdateFactory.newLatLng(station.location));
            }
            return true; // Trả về true để không hiện popup mặc định của Google Maps
        });

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
                    ((MainActivity) getActivity()).showStationCard(s.name, s.address, s.id);
                }
                break;
            }
        }
    }

    public void loadVehiclesByStation(int stationId) {
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

    private void showStationDetailDialog(MainActivity.BikeStation station) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_station_detail, null);

        // 1. Ánh xạ các View trong Dialog
        TextView tvName = view.findViewById(R.id.tvStationName);
        TextView tvAddress = view.findViewById(R.id.tvStationAddress);
        TextView tvBikeCount = view.findViewById(R.id.BikeCount);
        TextView tvMotorCount = view.findViewById(R.id.MotorCount);
        RecyclerView rvBikes = view.findViewById(R.id.RvBikes);
        RecyclerView rvMotors = view.findViewById(R.id.RvEMotors);

        tvName.setText(station.name);
        tvAddress.setText(station.address);

        // 2. Gọi API lấy danh sách xe của trạm này
        String url = APIHelper.BASE_URL + "vehicles/" + station.id;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<JSONObject> bikes = new ArrayList<>();
                    List<JSONObject> motors = new ArrayList<>();
                    try {
                        // Trong TramXeFragment.java
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject v = response.getJSONObject(i);
                            // Sử dụng trim() để loại bỏ khoảng trắng và toLowerCase() để đồng nhất
                            String typeFromDB = v.optString("type", "").trim().toLowerCase();

                            if ("bike".equals(typeFromDB)) {
                                bikes.add(v);
                            } else if ("motor".equals(typeFromDB)) { // Kiểm tra rõ ràng thay vì dùng else
                                motors.add(v);
                            }
                        }
                        // Hiển thị số lượng
                        tvBikeCount.setText("XE ĐẠP CƠ (" + bikes.size() + ")");
                        tvMotorCount.setText("XE MÁY ĐIỆN (" + motors.size() + ")");

                        // Đổ dữ liệu vào List cuộn ngang
                        rvBikes.setAdapter(new VehicleAdapter(bikes));
                        rvMotors.setAdapter(new VehicleAdapter(motors));



                        // 1. Cho xe đạp
                        VehicleAdapter bikeAdapter = new VehicleAdapter(bikes);
                        bikeAdapter.setOnItemClickListener(xe -> {
                            // GỌI HÀM HIỆN DIALOG Ở ĐÂY
                            showConfirmDialog(xe,station.name);
                        });
                        rvBikes.setAdapter(bikeAdapter);

                        // 2. Cho xe máy
                        VehicleAdapter motorAdapter = new VehicleAdapter(motors);
                        motorAdapter.setOnItemClickListener(xe -> {
                            // GỌI HÀM HIỆN DIALOG Ở ĐÂY
                            showConfirmDialog(xe,station.name);
                        });
                        rvMotors.setAdapter(motorAdapter);
                    } catch (Exception e) { e.printStackTrace(); }
                }, error -> Toast.makeText(getContext(), "Lỗi tải danh sách xe", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(requireContext()).add(request);

        // 3. Nút chỉ đường
        view.findViewById(R.id.btnDirection).setOnClickListener(v -> {
            dialog.dismiss();

            // Lấy vị trí hiện tại và vẽ đường đi trực tiếp trên bản đồ của app
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                        LatLng dest = station.location;

                        // Gọi hàm vẽ đường đi uốn lượn (sử dụng Directions API)
                        drawRealRoute(origin, dest);
                    } else {
                        Toast.makeText(requireContext(), "Không thể lấy vị trí hiện tại (Hãy chắc chắn GPS đang bật hoặc set vị trí giả trên máy ảo).", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Bạn cần cấp quyền vị trí để xem đường đi.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void drawRealRoute(LatLng origin, LatLng dest) {
        // Gọi backend của bạn, KHÔNG gọi Google trực tiếp
        String url = APIHelper.BASE_URL + "directions" +
                "?originLat=" + origin.latitude +
                "&originLng=" + origin.longitude +
                "&destLat=" + dest.latitude +
                "&destLng=" + dest.longitude;

        com.android.volley.toolbox.JsonObjectRequest request =
                new com.android.volley.toolbox.JsonObjectRequest(
                        com.android.volley.Request.Method.GET, url, null,
                        response -> {
                            try {
                                String status = response.getString("status");

                                if ("OK".equals(status)) {
                                    String encodedPoints = response.getString("encodedPolyline");
                                    String distance = response.optString("distance", "");
                                    String duration = response.optString("duration", "");

                                    java.util.List<LatLng> decodedPath =
                                            com.google.maps.android.PolyUtil.decode(encodedPoints);

                                    if (currentRoute != null) currentRoute.remove();

                                    currentRoute = mMap.addPolyline(
                                            new com.google.android.gms.maps.model.PolylineOptions()
                                                    .addAll(decodedPath)
                                                    .width(15f)
                                                    .color(android.graphics.Color.parseColor("#00B087"))
                                                    .startCap(new com.google.android.gms.maps.model.RoundCap())
                                                    .endCap(new com.google.android.gms.maps.model.RoundCap())
                                    );

                                    com.google.android.gms.maps.model.LatLngBounds.Builder builder =
                                            new com.google.android.gms.maps.model.LatLngBounds.Builder();
                                    for (LatLng point : decodedPath) builder.include(point);
                                    mMap.animateCamera(
                                            CameraUpdateFactory.newLatLngBounds(builder.build(), 200));

                                    // Hiện thông tin khoảng cách & thời gian
                                    if (!distance.isEmpty() && !duration.isEmpty()) {
                                        Toast.makeText(requireContext(),
                                                "Khoảng cách: " + distance + " • " + duration,
                                                Toast.LENGTH_LONG).show();
                                    }

                                } else {
                                    Toast.makeText(requireContext(),
                                            "Không tìm được đường đi", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(requireContext(),
                                        "Lỗi xử lý dữ liệu đường đi", Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> {
                            String msg = "Lỗi kết nối server";
                            if (error.networkResponse != null) {
                                msg += " (HTTP " + error.networkResponse.statusCode + ")";
                            }
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                );

        request.setShouldCache(false);
        com.android.volley.toolbox.Volley.newRequestQueue(
                requireContext().getApplicationContext()).add(request);
    }

    private void showConfirmDialog(JSONObject xe,String stationName) {
        // 1. Tạo Builder để build giao diện cho Dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        // 2. Nạp (Inflate) file XML giao diện bạn đã tạo (dialog_confirm_booking)
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_booking, null);
        builder.setView(view);

        // 3. Khởi tạo Dialog từ Builder
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // 4. LÀM TRONG SUỐT NỀN: Bước này cực kỳ quan trọng để thấy được góc bo tròn
        // của file background XML mà không bị cái khung trắng vuông của hệ thống đè lên.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 5. Ánh xạ các nút từ file XML
        Button btnLater = view.findViewById(R.id.btnLater);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        // Xử lý khi bấm "Để sau"
        btnLater.setOnClickListener(v -> dialog.dismiss());

        // Xử lý khi bấm "Xác nhận"
        btnConfirm.setOnClickListener(v -> {

            long currentTime = System.currentTimeMillis();
            if (currentTime < nextBookingTime) {
                // Tính số phút còn lại để thông báo cho người dùng
                long remainingMinutes = (nextBookingTime - currentTime) / (60 * 1000);
                long remainingSeconds = ((nextBookingTime - currentTime) / 1000) % 60;

                Toast.makeText(getContext(),
                        "Vui lòng đợi còn " + remainingMinutes + " phút " + remainingSeconds + " giây nữa",
                        Toast.LENGTH_LONG).show();
                return; // Thoát ra, không cho đặt xe
            }
            try {
                String plate = xe.getString("plate");
                dialog.dismiss(); // Đóng cái xác nhận
                showSuccessDialog(stationName, plate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 6. Hiển thị Dialog lên màn hình
        dialog.show();
    }

    private void showSuccessDialog(String stationName, String plate) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_booking_success, null);
        builder.setView(view);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Ánh xạ và đổ dữ liệu text động
        TextView tvMessage = view.findViewById(R.id.tvSuccessMessage);

        // Tạo chuỗi thông báo có định dạng (Bold tên trạm và biển số)
        String message = "Yêu cầu đặt giữ xe của bạn đã được xác nhận. Vui lòng di chuyển đến trạm " + stationName +
                ", biển số xe " + plate + " để nhận xe nhé!";
        tvMessage.setText(message);

        view.findViewById(R.id.btnCancelBooking).setOnClickListener(v -> {
            nextBookingTime = System.currentTimeMillis() + (5 * 60 * 1000);
            dialog.dismiss();
            showCancelSuccessDialog();
        });

        view.findViewById(R.id.btnGoToPickUp).setOnClickListener(v -> {
            // Logic khi người dùng nhấn đến nhận xe (ví dụ: mở chỉ đường)
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showCancelSuccessDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_cancel_booking, null);
        builder.setView(view);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnUnderstood).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
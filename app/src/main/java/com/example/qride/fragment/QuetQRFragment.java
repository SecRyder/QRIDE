package com.example.qride.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.qride.R;
import com.example.qride.thuexe.QRScanActivity;

public class QuetQRFragment extends Fragment {
    Button btnOpenScanner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // return inflater.inflate(R.layout.fragment_quet_qr, container, false);
        View view = inflater.inflate(R.layout.fragment_quet_qr, container, false);

        btnOpenScanner = view.findViewById(R.id.btnOpenScanner);

        btnOpenScanner.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QRScanActivity.class);
            startActivity(intent);
        });

        return view;
    }
}

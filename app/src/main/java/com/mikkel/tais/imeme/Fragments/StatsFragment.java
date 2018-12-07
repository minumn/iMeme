package com.mikkel.tais.imeme.Fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mikkel.tais.imeme.Models.Stats;
import com.mikkel.tais.imeme.R;
import com.mikkel.tais.imeme.Services.IMemeService;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Date;

public class StatsFragment extends Fragment {
    public IMemeService iMemeService;
    private boolean boundToIMemeService = false;
    private ServiceConnection serviceConnection;

    private static final String LOG_ID = "StatsFragment_log";
    private TextView txtFirstUsage;
    private TextView txtTotalBLBSeen;
    private TextView txtTotalBLBSaved;
    private TextView txtAvgBLBSeen;
    private TextView txtTotalGenSeen;
    private TextView txtTotalGenSaved;
    private TextView txtAvgGenDay;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initiateVariables();
        setupConnectionToIMemeService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unBindFromIMemeService();
    }

    private void initiateVariables() {
        txtFirstUsage = getActivity().findViewById(R.id.txtFirstUsage);
        txtTotalBLBSeen = getActivity().findViewById(R.id.txtTotalBLBSeen);
        txtTotalBLBSaved = getActivity().findViewById(R.id.txtTotalBLBSaved);
        txtAvgBLBSeen = getActivity().findViewById(R.id.txtAvgBLBSeen);
        txtTotalGenSeen = getActivity().findViewById(R.id.txtTotalGenSeen);
        txtTotalGenSaved = getActivity().findViewById(R.id.txtTotalGenSaved);
        txtAvgGenDay = getActivity().findViewById(R.id.txtAvgGenDay);
    }

    private String convertFloat(float f) {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.CEILING);

        return df.format(f);
    }

    // # # # SERVICE FUNCTIONALITY # # #
    private void setupConnectionToIMemeService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                iMemeService = ((IMemeService.IMemeUpdateServiceBinder) service).getService();
                Log.d(LOG_ID, "iMeme service connected.");

                Stats stats = iMemeService.getStatsFromSP();
                txtFirstUsage.setText(new Date(iMemeService.getFirstUsageFromSP()).toLocaleString());
                txtTotalBLBSeen.setText(Integer.toString(stats.getTotalBLBSeen()));
                txtTotalBLBSaved.setText(Integer.toString(stats.getTotalBLBSaved()));
                txtAvgBLBSeen.setText(convertFloat(stats.getTotalBLBAvgSeenDay()));
                txtTotalGenSeen.setText(Integer.toString(stats.getTotalGeneratedSeen()));
                txtTotalGenSaved.setText(Integer.toString(stats.getTotalGeneratedSaved()));
                txtAvgGenDay.setText(convertFloat(stats.getTotalGeneratedAvgSeenDay()));
            }

            public void onServiceDisconnected(ComponentName className) {
                iMemeService = null;
                Log.d(LOG_ID, "iMeme service disconnected.");
            }
        };

        bindToIMemeService();
    }

    private void bindToIMemeService() {
        Intent intent = new Intent(getActivity(), IMemeService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        boundToIMemeService = true;
    }

    private void unBindFromIMemeService() {
        if (boundToIMemeService) {
            getActivity().unbindService(serviceConnection);
            boundToIMemeService = false;
        }
    }
}
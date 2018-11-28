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

public class StatsFragment extends Fragment {
    public IMemeService iMemeService;
    private boolean boundToIMemeService = false;
    private ServiceConnection serviceConnection;

    private static final String LOG_ID = "StatsFragment_log";
    // TODO: Avg memes / day? Det er vel grunden til at vi gemmer første dag. Den er mærkelig i sig selv.
    private TextView txtFirstUsage;
    private TextView txtTotalBLBSeen;
    private TextView txtTotalBLBSaved;
    private TextView txtTotalBLBShared;

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
        txtTotalBLBShared = getActivity().findViewById(R.id.txtTotalBLBShared);
    }

    // # # # SERVICE FUNCTIONALITY # # #
    private void setupConnectionToIMemeService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                iMemeService = ((IMemeService.IMemeUpdateServiceBinder) service).getService();
                Log.d(LOG_ID, "iMeme service connected.");

                Stats stats = iMemeService.getStatsFromSP();
                txtFirstUsage.setText(iMemeService.getFirstUsageFromSP());
                txtTotalBLBSeen.setText(Integer.toString(stats.getTotalBLBSeen()));
                txtTotalBLBSaved.setText(Integer.toString(stats.getTotalBLBSaved()));
                txtTotalBLBShared.setText(Integer.toString(stats.getTotalBLBShared()));
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
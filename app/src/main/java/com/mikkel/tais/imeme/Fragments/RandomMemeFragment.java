package com.mikkel.tais.imeme.Fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.mikkel.tais.imeme.MainActivity;
import com.mikkel.tais.imeme.R;
import com.mikkel.tais.imeme.Services.IMemeService;

import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_NEW_BILL_MEME_AVAILABLE;

public class RandomMemeFragment extends Fragment {

    private static final String LOG_ID = "RandomMemeFragment_log";
    private RandomMemeViewModel mViewModel;
    private ImageView randomMemeImage;
    private Button backBtn, refreshMemeBtn;

    private RequestQueue volleyQueue;
    private BroadcastReceiver broadcastDataUpdatedReceiver;

    // Stuff for IMeme Service
    private ServiceConnection serviceConnection;
    public IMemeService iMemeService;
    private boolean boundToIMemeService = false;

    public static RandomMemeFragment newInstance() {
        return new RandomMemeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.random_meme_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(RandomMemeViewModel.class);
        // TODO: Use the ViewModel

        initiateVariables();
        setButtonFunctionality();
        setupConnectionToIMemeService();
        setupBroadcaster();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unBindFromIMemeService();
        unRegisterBroadcast(broadcastDataUpdatedReceiver);
    }

    private void setButtonFunctionality() {
        backBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), "Back button clicked!", Toast.LENGTH_SHORT).show();
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                }
        );

        refreshMemeBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), "Refresh meme clicked!", Toast.LENGTH_SHORT).show();
                        // TODO: Should prob use ViewModel?
                        iMemeService.requestRandomMeme();
                    }
                }
        );
    }

    private void initiateVariables() {
        randomMemeImage = getActivity().findViewById(R.id.randomMemeImageView);
        volleyQueue = Volley.newRequestQueue(getContext());
        backBtn = getActivity().findViewById(R.id.backBtn);
        refreshMemeBtn = getActivity().findViewById(R.id.newMemeBtn);

        // Set default picture
        if (randomMemeImage != null) {
            randomMemeImage.setImageResource(R.mipmap.ic_launcher);
        }
    }

    // # # # SERVICE FUNCTIONALITY # # #
    private void setupConnectionToIMemeService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                iMemeService = ((IMemeService.IMemeUpdateServiceBinder) service).getService();
                Log.d(LOG_ID, "iMeme service connected.");
                iMemeService.requestRandomMeme();
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

    // # # # BROADCAST # # #
    public void registerBroadcast(BroadcastReceiver broadcastDataUpdatedReceiver){
        Log.d(LOG_ID, "registering receivers");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_NEW_BILL_MEME_AVAILABLE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastDataUpdatedReceiver, filter);
    }

    public void unRegisterBroadcast(BroadcastReceiver broadcastDataUpdatedReceiver){
        Log.d(LOG_ID, "unregistering receivers");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastDataUpdatedReceiver);
    }

    private void setupBroadcaster(){
        broadcastDataUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOG_ID, "");
                String result = intent.getStringExtra(IMemeService.BROADCAST_RESULT);

                if(result == null) {
                    Log.d(LOG_ID, "result from broadcast is null. This should not happen");
                    result = "";
                }
                if(boundToIMemeService) {
                    randomMemeImage.setImageBitmap(iMemeService.getRandomMeme());
                }
            }
        };
        registerBroadcast(broadcastDataUpdatedReceiver);
    }
}

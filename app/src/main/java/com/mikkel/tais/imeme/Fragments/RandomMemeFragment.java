package com.mikkel.tais.imeme.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import com.mikkel.tais.imeme.CapturePhotoUtils;
import com.mikkel.tais.imeme.R;
import com.mikkel.tais.imeme.Services.IMemeService;

import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_NEW_BILL_MEME_AVAILABLE;

public class RandomMemeFragment extends Fragment {

    private static final String LOG_ID = "RandomMemeFragment_log";
    private RandomMemeViewModel mViewModel;
    private ImageView randomMemeImage;
    private Button backBtn, refreshMemeBtn, btnSave;
    private Bitmap currentMeme;
    private boolean savedState = false;

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

        if (savedInstanceState != null) {
            savedState = true;
        }

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

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Save image from MainActivity through service
                if (checkPermissionWRITE_EXTERNAL_STORAGE(getContext())) {
                    CapturePhotoUtils.insertImage(getActivity().getContentResolver(), currentMeme, "hej", "dav");
                }
            }
        });
    }

    public static final int WRITE_EXTERNAL_STORAGE_REQ = 134;

    // Permission checker and showDialog from https://stackoverflow.com/questions/37672338/java-lang-securityexception-permission-denial-reading-com-android-providers-me
    public boolean checkPermissionWRITE_EXTERNAL_STORAGE(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;

        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showDialog("External storage", context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            WRITE_EXTERNAL_STORAGE_REQ);
                }

                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public void showDialog(final String msg, final Context context, final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{permission},
                                WRITE_EXTERNAL_STORAGE_REQ);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    private void initiateVariables() {
        randomMemeImage = getActivity().findViewById(R.id.randomMemeImageView);
        volleyQueue = Volley.newRequestQueue(getContext());
        backBtn = getActivity().findViewById(R.id.backBtn);
        refreshMemeBtn = getActivity().findViewById(R.id.newMemeBtn);
        btnSave = getActivity().findViewById(R.id.btnSaveBill);

        // Set default picture
        if (randomMemeImage != null) {
            randomMemeImage.setImageResource(R.mipmap.ic_launcher);
        }
    }

    public void getRandomMeme() {
        final String imageUrl = "https://belikebill.ga/billgen-API.php?default=1";
        // TODO: Should be in the service
        // REF: Inspiration found on https://android--examples.blogspot.com/2017/02/android-volley-image-request-example.html
        // TODO: Could look at above link how they save files.

        ImageRequest imageRequest = new ImageRequest(
                imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        // TODO: Do something with response! Maybe save it locally and send a broadcast with link.
                        randomMemeImage.setImageBitmap(response);
                        currentMeme = response;
                    }
                },
                0, // Image width
                0, // Image height
                ImageView.ScaleType.CENTER_CROP, // Image scale type
                Bitmap.Config.RGB_565, //Image decode configuration
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
    }

    // # # # SERVICE FUNCTIONALITY # # #
    private void setupConnectionToIMemeService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                iMemeService = ((IMemeService.IMemeUpdateServiceBinder) service).getService();
                Log.d(LOG_ID, "iMeme service connected.");
                if (!savedState) {
                    iMemeService.requestRandomMeme();
                } else {
                    randomMemeImage.setImageBitmap(iMemeService.getRandomMeme());
                }
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
    public void registerBroadcast(BroadcastReceiver broadcastDataUpdatedReceiver) {
        Log.d(LOG_ID, "registering receivers");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_NEW_BILL_MEME_AVAILABLE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastDataUpdatedReceiver, filter);
    }

    public void unRegisterBroadcast(BroadcastReceiver broadcastDataUpdatedReceiver) {
        Log.d(LOG_ID, "unregistering receivers");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastDataUpdatedReceiver);
    }

    private void setupBroadcaster() {
        broadcastDataUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOG_ID, "");
                String result = intent.getStringExtra(IMemeService.BROADCAST_RESULT);

                if (result == null) {
                    Log.d(LOG_ID, "result from broadcast is null. This should not happen");
                    result = "";
                }
                if (boundToIMemeService) {
                    randomMemeImage.setImageBitmap(iMemeService.getRandomMeme());
                }
            }
        };

        registerBroadcast(broadcastDataUpdatedReceiver);
    }
}
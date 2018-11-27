package com.mikkel.tais.imeme.Fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.mikkel.tais.imeme.MemeResultActivity;
import com.mikkel.tais.imeme.Models.Meme;
import com.mikkel.tais.imeme.Models.Stats;
import com.mikkel.tais.imeme.R;
import com.mikkel.tais.imeme.Services.IMemeService;
import com.mikkel.tais.imeme.Utils.MemeAdaptor;
import com.mikkel.tais.imeme.Utils.MemeJsonParser;

import java.util.List;

import static com.mikkel.tais.imeme.MemeResultActivity.EXTRA_MEME_ID;
import static com.mikkel.tais.imeme.MemeResultActivity.EXTRA_MEME_T1;
import static com.mikkel.tais.imeme.MemeResultActivity.EXTRA_MEME_T2;
import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_LIST_OF_MEMES;
import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_MEME_LIST_RESULT;
import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_NEW_BILL_MEME_AVAILABLE;

public class MemeGeneratorFragment extends Fragment {
    public IMemeService iMemeService;
    private boolean boundToIMemeService = false;
    private ServiceConnection serviceConnection;
    private BroadcastReceiver broadcastDataUpdatedReceiver;

    private static final String LOG_ID = "MemeGenFrag_log";
    private EditText edtTopText;
    private EditText edtBottomText;
    private ListView lstMemes;
    private List<Meme> memes;
    private MemeAdaptor adaptor;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meme_generator, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initiateVariables();
        setupConnectionToIMemeService();
        setupBroadcaster();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unBindFromIMemeService();
        unRegisterBroadcast(broadcastDataUpdatedReceiver);
    }

    private void goToResult(Meme meme) {
        String topText = edtTopText.getText().toString().trim();
        String bottomText = edtBottomText.getText().toString().trim();

        if (topText.equals("") || bottomText.equals("")) {
            //TODO: Externalizeeeee!
            Toast.makeText(getActivity(), "Fill out the text fields", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(getContext(), MemeResultActivity.class);
            intent.putExtra(EXTRA_MEME_ID, meme.getId());
            intent.putExtra(EXTRA_MEME_T1, topText);
            intent.putExtra(EXTRA_MEME_T2, bottomText);

            startActivity(intent);
        }
    }

    private void initiateVariables() {
        edtTopText = getActivity().findViewById(R.id.edtTopText);
        edtBottomText = getActivity().findViewById(R.id.edtBottomText);
        lstMemes = getActivity().findViewById(R.id.lstMemes);
    }

    // # # # SERVICE FUNCTIONALITY # # #
    private void setupConnectionToIMemeService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                iMemeService = ((IMemeService.IMemeUpdateServiceBinder) service).getService();
                Log.d(LOG_ID, "iMeme service connected.");

                iMemeService.requestListOfMemes();

                adaptor = new MemeAdaptor(getContext(), memes);
                lstMemes.setAdapter(adaptor);
                lstMemes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Meme meme = memes.get(position);
                        if (meme != null) {
                            goToResult(meme);
                        }
                    }
                });
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
        filter.addAction(BROADCAST_LIST_OF_MEMES);
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
                String result = intent.getStringExtra(BROADCAST_MEME_LIST_RESULT);

                if (result == null) {
                    Log.d(LOG_ID, "result from broadcast is null. This should not happen");
                } else {
                    memes = MemeJsonParser.parseMemeJson(result);
                    adaptor.setMemes(memes);
                    adaptor.notifyDataSetChanged();
                }
            }
        };

        registerBroadcast(broadcastDataUpdatedReceiver);
    }
}

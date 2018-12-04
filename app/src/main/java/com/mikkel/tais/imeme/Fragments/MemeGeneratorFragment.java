package com.mikkel.tais.imeme.Fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private ListView lstMemes;
    private List<Meme> memes;
    private MemeAdaptor adaptor;

    private InputFilter filter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            //REF: https://stackoverflow.com/questions/21828323/how-can-restrict-my-edittext-input-to-some-special-character-like-backslash-t
            String blockCharacterSet = "~#^|$%&*";
            if (source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }

            return null;
        }
    };

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

    private void openTextDialog(final Meme meme) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMessage(meme.getName());

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText edtText1 = setupDialogEditText(getText(R.string.edt_top_text).toString());
        layout.addView(edtText1);

        final EditText edtText2 = setupDialogEditText(getText(R.string.edt_bottom_text).toString());
        layout.addView(edtText2);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String text1 = edtText1.getText().toString().trim();
                String text2 = edtText2.getText().toString().trim();

                if (text1.equals("") || text2.equals("")) {
                    Toast.makeText(getActivity(), getText(R.string.lbl_fill_out_all_fields), Toast.LENGTH_SHORT).show();
                } else {
                    goToResult(meme, text1, text2);
                }
            }
        });

        builder.setNegativeButton(getText(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.create().show();
    }

    private EditText setupDialogEditText(String hint) {
        final EditText text = new EditText(getContext());
        text.setHint(hint);
        text.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50), filter});

        return text;
    }

    private void goToResult(Meme meme, String text1, String text2) {
        Intent intent = new Intent(getContext(), MemeResultActivity.class);
        intent.putExtra(EXTRA_MEME_ID, meme.getId());
        intent.putExtra(EXTRA_MEME_T1, text1);
        intent.putExtra(EXTRA_MEME_T2, text2);

        startActivity(intent);
    }

    private void initiateVariables() {
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
                            openTextDialog(meme);
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

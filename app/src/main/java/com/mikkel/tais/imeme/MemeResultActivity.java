package com.mikkel.tais.imeme;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.mikkel.tais.imeme.Services.IMemeService;

import org.json.JSONException;
import org.json.JSONObject;

import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_GENERATED_MEME;
import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_GENERATED_MEME_IMG;
import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_GENERATED_MEME_IMG_RESULT;
import static com.mikkel.tais.imeme.Services.IMemeService.BROADCAST_GENERATED_MEME_RESULT;

public class MemeResultActivity extends AppCompatActivity {
    private IMemeService iMemeService;
    private boolean boundToIMemeService = false;
    private ServiceConnection serviceConnection;
    private BroadcastReceiver broadcastDataUpdatedReceiver;

    public static final String EXTRA_MEME_ID = "extra_meme_id";
    public static final String EXTRA_MEME_T1 = "extra_meme_t1";
    public static final String EXTRA_MEME_T2 = "extra_meme_t2";
    private static final String LOG_ID = "MemeResActivity_log";
    private ImageView generatedMemeImage;
    private Bitmap generatedMeme;
    private Button btnSave;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meme_result);

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

    private void initiateVariables() {
        Intent data = getIntent();
        String id = data.getStringExtra(EXTRA_MEME_ID);
        String t1 = data.getStringExtra(EXTRA_MEME_T1);
        String t2 = data.getStringExtra(EXTRA_MEME_T2);

        String topText = t1.replace(" ", "%20");
        String bottomText = t2.replace(" ", "%20");

        url = "https://api.imgflip.com/caption_image?template_id=" + id
                + "&username=imgflip_hubot&password=imgflip_hubot&text0=" + topText
                + "&text1=" + bottomText;

        generatedMemeImage = findViewById(R.id.imgGenMeme);
        btnSave = findViewById(R.id.btnSaveGenMeme);
    }

    private void setButtonFunctionality() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: update with new shared prefs data
                //int number = iMemeService.getStatsFromSP().getTotalBLBSaved();
                //String title = "BeLikeBill_" + number;

                iMemeService.saveImageToStorage(generatedMeme, "");
            }
        });
    }

    private void updateMeme(Bitmap meme) {
        generatedMemeImage.setImageBitmap(meme);
        generatedMeme = meme;
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }

    // # # # SERVICE FUNCTIONALITY # # #
    private void setupConnectionToIMemeService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                iMemeService = ((IMemeService.IMemeUpdateServiceBinder) service).getService();
                Log.d(LOG_ID, "iMeme service connected.");

                iMemeService.requestGeneratedMeme(url);
            }

            public void onServiceDisconnected(ComponentName className) {
                iMemeService = null;
                Log.d(LOG_ID, "iMeme service disconnected.");
            }
        };

        bindToIMemeService();
    }

    private void bindToIMemeService() {
        Intent intent = new Intent(MemeResultActivity.this, IMemeService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        boundToIMemeService = true;
    }

    private void unBindFromIMemeService() {
        if (boundToIMemeService) {
            unbindService(serviceConnection);
            boundToIMemeService = false;
        }
    }

    // # # # BROADCAST # # #
    public void registerBroadcast(BroadcastReceiver broadcastDataUpdatedReceiver) {
        Log.d(LOG_ID, "registering receivers");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_GENERATED_MEME);
        filter.addAction(BROADCAST_GENERATED_MEME_IMG);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastDataUpdatedReceiver, filter);
    }

    public void unRegisterBroadcast(BroadcastReceiver broadcastDataUpdatedReceiver) {
        Log.d(LOG_ID, "unregistering receivers");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastDataUpdatedReceiver);
    }

    private void setupBroadcaster() {
        broadcastDataUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String jsonResponse = intent.getStringExtra(BROADCAST_GENERATED_MEME_RESULT);
                String image = intent.getStringExtra(BROADCAST_GENERATED_MEME_IMG_RESULT);
                Log.d(LOG_ID, "receive broadcast json: " + jsonResponse);
                Log.d(LOG_ID, "receive broadcast image: " + image);

                if (jsonResponse != null) {
                    try {
                        JSONObject json = new JSONObject(jsonResponse);

                        if (json.getBoolean("success")) {
                            String imageUrl = json.getJSONObject("data")
                                    .getString("url")
                                    .replace("\\", "")
                                    .replace("http", "https");
                            iMemeService.requestGeneratedMemeImg(imageUrl);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (image != null) {
                    updateMeme(iMemeService.getGeneratedMeme());
                } else {
                    Log.d(LOG_ID, "result from broadcast is null. This should not happen");
                }
            }
        };

        registerBroadcast(broadcastDataUpdatedReceiver);
    }
}
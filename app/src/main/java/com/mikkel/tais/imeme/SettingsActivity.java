package com.mikkel.tais.imeme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mikkel.tais.imeme.Services.IMemeService;

public class SettingsActivity extends AppCompatActivity {

    private static final String LOG_ID = "SettingActivity_log";
    SeekBar timeSliderEnd, timeSliderStart;
    TextView silentTimeTxt;
    Integer silentTimeStart = 0, silentTimeEnd = 0;
    boolean notificationLevel = true;
    Button backBtn, saveBtn, resetStatsBtn;
    Switch notificationSwitch;

    // Stuff for IMeme Service
    public IMemeService iMemeService;
    private ServiceConnection serviceConnection;
    private boolean boundToIMemeService = false;

    // # # # onFunctions # # #
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupConnectionToIMemeService();
        bindToIMemeService();

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        timeSliderStart = findViewById(R.id.seekBarSilentStart);
        timeSliderStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    silentTimeStart = progress;
                    updateSilentTimeTxt();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        timeSliderEnd = findViewById(R.id.seekBarSilentEnd);
        timeSliderEnd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    silentTimeEnd = progress;
                    updateSilentTimeTxt();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        silentTimeTxt = findViewById(R.id.silentTimeTxt);
        silentTimeTxt.setText("None");

        initSwitch();
        
        initButtonFunctionality();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unBindFromIMemeService();
    }

    // # # # Utility functions # # #
    private void updateSilentTimeTxt() {
        if (silentTimeStart < silentTimeEnd) {
            silentTimeTxt.setText(String.format("%02d:%02d-%02d:%02d", silentTimeStart / 60, silentTimeStart % 60, silentTimeEnd / 60, silentTimeEnd % 60));
        } else {
            silentTimeTxt.setText("None");
        }
    }

    private void initButtonFunctionality() {
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveNotificationsBtn);
        resetStatsBtn = findViewById(R.id.resetStatsBtn);

        backBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                }
        );

        saveBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveClicked();
                    }
                }
        );

        resetStatsBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetStatsClicked();

                    }
                }
        );
    }

    private void resetStatsClicked() {
        if(boundToIMemeService){
            // TODO: Ask if user is sure
            iMemeService.resetStats();
            Toast.makeText(this, "Stats reset", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveClicked() {
        Toast.makeText(this, "Silent time saved", Toast.LENGTH_SHORT).show();
        iMemeService.setSilentTime(silentTimeStart, silentTimeEnd);
    }

    private void initSwitch(){
        notificationSwitch = findViewById(R.id.notifications_switch);
        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (boundToIMemeService){
                    notificationLevel = isChecked;
                    updateSilentTimeTxt();
                    iMemeService.setNotificationLevel(isChecked);
                    // Toast.makeText(SettingsActivity.this, "Switch ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // # # # SERVICE FUNCTIONALITY # # #
    private void setupConnectionToIMemeService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                iMemeService = ((IMemeService.IMemeUpdateServiceBinder) service).getService();
                Log.d(LOG_ID, "iMeme service connected.");

                // Get data
                silentTimeStart = iMemeService.getSilentTimeStart();
                silentTimeEnd = iMemeService.getSilentTimeEnd();
                notificationLevel = iMemeService.getNotificationLevel();

                timeSliderStart.setProgress(silentTimeStart);
                timeSliderEnd.setProgress(silentTimeEnd);
                notificationSwitch.setChecked(notificationLevel);

                // Show user
                updateSilentTimeTxt();
            }

            public void onServiceDisconnected(ComponentName className) {
                iMemeService = null;
                Log.d(LOG_ID, "iMeme service disconnected.");
            }
        };
    }

    private void bindToIMemeService() {
        Intent intent = new Intent(SettingsActivity.this, IMemeService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        boundToIMemeService = true;
    }

    private void unBindFromIMemeService() {
        if (boundToIMemeService) {
            unbindService(serviceConnection);
            boundToIMemeService = false;
        }
    }

}

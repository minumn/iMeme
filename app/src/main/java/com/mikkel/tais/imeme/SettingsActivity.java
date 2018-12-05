package com.mikkel.tais.imeme;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
    private SeekBar timeSliderEnd, timeSliderStart;
    private TextView silentTimeTxt;
    private Integer silentTimeStart = 0, silentTimeEnd = 0;
    private boolean notificationLevel = true;
    private Switch notificationSwitch;

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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        timeSliderStart = findViewById(R.id.seekBarSilentStart);
        timeSliderStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
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
        silentTimeTxt.setText(getText(R.string.lbl_silent_time));

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
        if (silentTimeStart.equals(silentTimeEnd)) {
            silentTimeTxt.setText(getText(R.string.lbl_silent_time));
        } else {
            silentTimeTxt.setText(String.format("%02d:%02d-%02d:%02d", silentTimeStart / 60, silentTimeStart % 60, silentTimeEnd / 60, silentTimeEnd % 60));
        }
    }

    private void initButtonFunctionality() {
        Button saveBtn = findViewById(R.id.saveNotificationsBtn);
        Button resetStatsBtn = findViewById(R.id.resetStatsBtn);

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
        if (boundToIMemeService) {
            showDialog();
        }
    }

    private void showDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(getText(R.string.lbl_reset_stats));
        alertBuilder.setMessage(getText(R.string.lbl_reset_stats_confirm));

        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                iMemeService.resetStats();
            }
        });

        alertBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alertBuilder.create().show();
    }

    private void saveClicked() {
        Toast.makeText(this, getText(R.string.lbl_silent_time_saved), Toast.LENGTH_SHORT).show();
        iMemeService.setSilentTime(silentTimeStart, silentTimeEnd);
    }

    private void initSwitch() {
        notificationSwitch = findViewById(R.id.notifications_switch);
        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (boundToIMemeService) {
                    notificationLevel = isChecked;
                    updateSilentTimeTxt();
                    iMemeService.setNotificationLevel(isChecked);
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
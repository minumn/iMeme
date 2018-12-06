package com.mikkel.tais.imeme.Services;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mikkel.tais.imeme.MainActivity;
import com.mikkel.tais.imeme.Models.Stats;
import com.mikkel.tais.imeme.R;
import com.mikkel.tais.imeme.Utils.CapturePhotoUtils;

import java.util.Calendar;

import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_BOOL_FIRST_TIME;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_BOOL_NOTI;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_FLOAT_BLB_AVG_SEEN_DAY;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_BLB_SAVED;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_BLB_SEEN;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_BLB_SHARED;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_FLOAT_GEN_AVG_SEEN_DAY;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_GEN_SAVED;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_GEN_SEEN;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_GEN_SHARED;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_SILENT_END;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_SILENT_START;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_LONG_FIRST_TIME;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_NAME;

/**
 * This Service is supposed to handle URL calls getting Memes as well as nofitications for the user.
 * The Service should be persistent so it can send notifications when the app is not open.
 */
public class IMemeService extends Service {
    // TODO: Make Service persistent

    private static final String CHANNEL_ID = "IMemeServiceNotification";
    public static final String BROADCAST_NEW_BILL_MEME_AVAILABLE = "broadcast_new_bill_meme_available";
    public static final String BROADCAST_LIST_OF_MEMES = "broadcast_list_of_memes";
    public static final String BROADCAST_GENERATED_MEME = "broadcast_generated_meme";
    public static final String BROADCAST_GENERATED_MEME_IMG = "broadcast_generated_meme_img";
    public static final String BLB_SAVE_TITLE = "BeLikeBill_";
    public static final String GENERATED_SAVE_TITLE = "iMemeGen_";

    public static final String BROADCAST_RESULT = "broadcast_result";
    public static final String BROADCAST_MEME_LIST_RESULT = "broadcast_meme_list_result";
    public static final String BROADCAST_GENERATED_MEME_RESULT = "broadcast_generated_meme_result";
    public static final String BROADCAST_GENERATED_MEME_IMG_RESULT = "broadcast_generated_meme_img_result";
    public static final int WRITE_EXTERNAL_STORAGE_REQ = 134;
    private final IBinder binder = new IMemeUpdateServiceBinder();
    private static final String LOG_ID = "iMemeService_log";
    private Bitmap randomBillMeme;
    private Bitmap generatedMeme;

    // Stats variable
    public SharedPreferences prefs;
    private int totalBLBSeen;
    private int totalBLBSaved;
    private int totalGeneratedSeen;
    private int totalGeneratedSaved;

    // Volley stuff
    private RequestQueue volleyQueue;

    // Notification stuff
    NotificationManagerCompat notificationManager;
    private static final int NOTIFICATION_ID = 101;
    private int silentTimeStart, silentTimeEnd;
    private boolean notificationLevel;
    private static final long NOTIFICATION_DELAY = 1000 * 60 * 60; // 60 minutes

    Handler notificationHandler = new Handler();
    Runnable notificationRunnable = new Runnable() {
        @Override
        public void run() {
            calcAvgPerDayStats();
            notifyUserAboutNewMeme();
            notificationHandler.postDelayed(notificationRunnable, NOTIFICATION_DELAY);
        }
    };

    // # # # Setup functions # # #
    public IMemeService() {
    }

    public class IMemeUpdateServiceBinder extends Binder {
        //return ref to service (or at least an interface) that activity can call public methods on
        public IMemeService getService() {
            return IMemeService.this;
        }
    }

    // # # # onFunctions # # #
    @Override
    //very important! return your IBinder (your custom Binder)
    public IBinder onBind(Intent intent) {
        Log.d(LOG_ID, "StockService binder returned");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_ID, "iMemeService has been created.");

        // Init stuff
        volleyQueue = Volley.newRequestQueue(this);
        notificationManager = NotificationManagerCompat.from(this);
        setupSharedPrefs();
        loadStatsVariables();
        calcAvgPerDayStats();

        // Load notificationVariables
        loadNotificationVariables();

        // Very important on Android 8.0 and higher to create notificationChannel!
        createNotificationChannel();
        notificationHandler.postDelayed(notificationRunnable, NOTIFICATION_DELAY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_ID, "iMemeService has been destroyed.");
    }

    // # # # Functionality functions # # #
    public Bitmap getRandomMeme() {
        return randomBillMeme;
    }

    private void loadNotificationVariables() {
        int defValue = -1;
        int prefsSilentTimeStart = prefs.getInt(SHARED_PREFS_KEY_INT_SILENT_START, defValue);
        int prefsSilentTimeEnd = prefs.getInt(SHARED_PREFS_KEY_INT_SILENT_END, defValue);

        if (prefsSilentTimeStart == defValue) {
            silentTimeStart = 22 * 60;
            silentTimeEnd = 8 * 60;
        } else {
            silentTimeStart = prefsSilentTimeStart;
            silentTimeEnd = prefsSilentTimeEnd;
        }

        notificationLevel = prefs.getBoolean(SHARED_PREFS_KEY_BOOL_NOTI, true);
    }

    private void loadStatsVariables() {
        if (prefs.getBoolean(SHARED_PREFS_KEY_BOOL_FIRST_TIME, true)) {
            setSharedPref(SHARED_PREFS_KEY_BOOL_FIRST_TIME, false);
            setSharedPref(SHARED_PREFS_KEY_LONG_FIRST_TIME, Calendar.getInstance().getTimeInMillis());
        }

        Stats stats = getStatsFromSP();
        totalBLBSeen = stats.getTotalBLBSeen();
        totalBLBSaved = stats.getTotalBLBSaved();
        totalGeneratedSeen = stats.getTotalGeneratedSeen();
        totalGeneratedSaved = stats.getTotalGeneratedSaved();
    }

    private void calcAvgPerDayStats() {
        long diff = Calendar.getInstance().getTimeInMillis() - prefs.getLong(SHARED_PREFS_KEY_LONG_FIRST_TIME, 0);
        float diffDays;
        if (diff > 86400000) {
            diffDays = diff / 86400000; //1 day in millis
        } else {
            diffDays = (float) 1.0;
        }

        float avgBLB = totalBLBSeen / diffDays;
        float avgGen = totalGeneratedSeen / diffDays;

        if (Float.isInfinite(avgBLB) || Float.isNaN(avgBLB)) {
            avgBLB = (float) 0.0;
        }

        if (Float.isInfinite(avgGen) || Float.isNaN(avgGen)) {
            avgGen = (float) 0.0;
        }

        setSharedPref(SHARED_PREFS_KEY_FLOAT_BLB_AVG_SEEN_DAY, avgBLB);
        setSharedPref(SHARED_PREFS_KEY_FLOAT_GEN_AVG_SEEN_DAY, avgGen);
    }


    // REF: Inspiration found on https://android--examples.blogspot.com/2017/02/android-volley-image-request-example.html
    public void requestRandomMeme() {
        final String imageUrl = "https://belikebill.ga/billgen-API.php?default=1";

        ImageRequest imageRequest = new ImageRequest(
                imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        randomBillMeme = response;
                        broadcastNewBillMemeAvailable("OK");
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
                }
        );

        volleyQueue.add(imageRequest);
    }

    // REF: https://developer.android.com/training/volley/simple
    public void requestListOfMemes() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://api.imgflip.com/get_memes",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        broadcastListOfAvailableMemes(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        broadcastListOfAvailableMemes(null);
                    }
                });

        volleyQueue.add(stringRequest);
    }

    public void requestGeneratedMeme(String url) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        broadcastGeneratedMeme(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        broadcastGeneratedMeme(null);
                    }
                });

        volleyQueue.add(stringRequest);
    }

    public Bitmap getGeneratedMeme() {
        return generatedMeme;
    }

    public void requestGeneratedMemeImg(String imageUrl) {
        ImageRequest imageRequest = new ImageRequest(
                imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        generatedMeme = response;
                        broadcastGeneratedMemeImg("OK");
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
                }
        );

        volleyQueue.add(imageRequest);
    }

    public void saveImageToStorage(Bitmap source, String title, Activity activity) {
        String url = getText(R.string.lbl_image_not_saved).toString();

        if (checkPermissionWRITE_EXTERNAL_STORAGE(this, activity)) {
            CapturePhotoUtils.insertImage(getContentResolver(), source, title, getText(R.string.lbl_image_gen_from_imeme).toString());
            url = getText(R.string.lbl_image_saved).toString();

            if (title.contains(BLB_SAVE_TITLE)) {
                totalBLBSaved += 1;
                setSharedPref(SHARED_PREFS_KEY_INT_BLB_SAVED, totalBLBSaved);
            } else if (title.contains(GENERATED_SAVE_TITLE)) {
                totalGeneratedSaved += 1;
                setSharedPref(SHARED_PREFS_KEY_INT_GEN_SAVED, totalGeneratedSaved);
            } else {
                Log.d(LOG_ID, "Stats not updated for " + title);
            }
        }

        Toast.makeText(this, url, Toast.LENGTH_SHORT).show();
    }

    // Permission checker and showDialog from https://stackoverflow.com/questions/37672338/java-lang-securityexception-permission-denial-reading-com-android-providers-me
    private boolean checkPermissionWRITE_EXTERNAL_STORAGE(final Context context, Activity activity) {
        int currentAPIVersion = Build.VERSION.SDK_INT;

        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showDialog(context);
                } else {
                    ActivityCompat.requestPermissions(
                            activity,
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

    private void showDialog(final Context context) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle(getText(R.string.lbl_perm_necessary));
        alertBuilder.setMessage(getText(R.string.lbl_external_storage) + " "
                + getText(R.string.lbl_perm_is_necessary) + " "
                + getText(R.string.lbl_save_image_again));
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_EXTERNAL_STORAGE_REQ);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    // # # # Notifications # # #
    // REF: https://developer.android.com/training/notify-user/build-notification
    private void notifyUserAboutNewMeme() {
        boolean throw_notification = notificationLevel && silentTimeNotNow();
        Log.d(LOG_ID, "notifyUserAboutNewMeme called. Throw_notification: " + throw_notification);
        if (throw_notification) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction("bill");
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_menu_share)
                    .setContentTitle("iMeme")
                    .setContentText(getText(R.string.lbl_noti_msg))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    // Following two lines makes you able to tap on the notification to shoot pendingIntent
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    /**
     * This function checks if current time is in the silent time interval.
     *
     * @return boolean value for above.
     */
    private boolean silentTimeNotNow() {
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(cal.HOUR_OF_DAY), currentMinute = cal.get(cal.MINUTE);

        int silentTimeStartHour = silentTimeStart / 60, silentTimeStartMinute = silentTimeStart % 60;
        int silentTimeEndHour = silentTimeEnd / 60, silentTimeEndMinute = silentTimeEnd % 60;

        // All is good. We are outside the silentTime.
        if (currentHour < silentTimeStartHour || currentHour > silentTimeEndHour) {
            return true;
        } else {
            // We are close to startTime / endTime
            if (currentHour == silentTimeStartHour) {
                return currentMinute < silentTimeStartMinute;
            } else if (currentHour == silentTimeEndHour) {
                return currentMinute > silentTimeEndMinute;
            } else {
                Log.d(LOG_ID, "Error in interpretting silentTimeNotNow()");

                return false;
            }
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void setNotificationLevel(boolean level) {
        notificationLevel = level;
        setSharedPref(SHARED_PREFS_KEY_BOOL_NOTI, notificationLevel);
    }

    public void setSilentTime(int silentTimeStart_, int silentTimeEnd_) {
        silentTimeStart = silentTimeStart_;
        silentTimeEnd = silentTimeEnd_;

        setSharedPref(SHARED_PREFS_KEY_INT_SILENT_START, silentTimeStart);
        setSharedPref(SHARED_PREFS_KEY_INT_SILENT_END, silentTimeEnd);

        Toast.makeText(this, getText(R.string.lbl_silent_time_saved), Toast.LENGTH_SHORT).show();
    }

    public boolean getNotificationLevel() {
        return notificationLevel;
    }

    public int getSilentTimeStart() {
        return silentTimeStart;
    }

    public int getSilentTimeEnd() {
        return silentTimeEnd;
    }

    // # # # BROADCAST # # #
    private void broadcastNewBillMemeAvailable(String result) {
        Log.d(LOG_ID, "Broadcasting new bill meme available!");
        Intent intent = new Intent(BROADCAST_NEW_BILL_MEME_AVAILABLE);
        intent.putExtra(BROADCAST_RESULT, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        totalBLBSeen += 1;
        setSharedPref(SHARED_PREFS_KEY_INT_BLB_SEEN, totalBLBSeen);
        calcAvgPerDayStats();
    }

    private void broadcastListOfAvailableMemes(String result) {
        Log.d(LOG_ID, "Broadcasting list of available memes!");
        Intent intent = new Intent(BROADCAST_LIST_OF_MEMES);
        intent.putExtra(BROADCAST_MEME_LIST_RESULT, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastGeneratedMeme(String result) {
        Log.d(LOG_ID, "Broadcasting list of available memes!");
        Intent intent = new Intent(BROADCAST_GENERATED_MEME);
        intent.putExtra(BROADCAST_GENERATED_MEME_RESULT, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastGeneratedMemeImg(String result) {
        Log.d(LOG_ID, "Broadcasting generated meme!");
        Intent intent = new Intent(BROADCAST_GENERATED_MEME_IMG);
        intent.putExtra(BROADCAST_GENERATED_MEME_IMG_RESULT, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        totalGeneratedSeen += 1;
        setSharedPref(SHARED_PREFS_KEY_INT_GEN_SEEN, totalGeneratedSeen);
        calcAvgPerDayStats();
    }

    // # # # Functions for StatsActivity # # #
    private void setupSharedPrefs() {
        prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
    }

    public Stats getStatsFromSP() {
        Stats stats = new Stats();

        stats.setTotalBLBSeen(prefs.getInt(SHARED_PREFS_KEY_INT_BLB_SEEN, 0));
        stats.setTotalBLBSaved(prefs.getInt(SHARED_PREFS_KEY_INT_BLB_SAVED, 0));
        stats.setTotalBLBShared(prefs.getInt(SHARED_PREFS_KEY_INT_BLB_SHARED, 0));
        stats.setTotalBLBAvgSeenDay(prefs.getFloat(SHARED_PREFS_KEY_FLOAT_BLB_AVG_SEEN_DAY, 0));

        stats.setTotalGeneratedSeen(prefs.getInt(SHARED_PREFS_KEY_INT_GEN_SEEN, 0));
        stats.setTotalGeneratedSaved(prefs.getInt(SHARED_PREFS_KEY_INT_GEN_SAVED, 0));
        stats.setTotalGeneratedShared(prefs.getInt(SHARED_PREFS_KEY_INT_GEN_SHARED, 0));
        stats.setTotalGeneratedAvgSeenDay(prefs.getFloat(SHARED_PREFS_KEY_FLOAT_GEN_AVG_SEEN_DAY, 0));

        return stats;
    }

    public long getFirstUsageFromSP() {
        return prefs.getLong(SHARED_PREFS_KEY_LONG_FIRST_TIME, 0);
    }

    public void setSharedPref(String key, Object value) {
        SharedPreferences.Editor editor = prefs.edit();

        if (value instanceof Integer) {
            editor.putInt(key, (int) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (boolean) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (float) value);
        }

        editor.apply();
    }

    public void resetStats() {
        totalBLBSeen = 0;
        totalGeneratedSeen = 0;

        setSharedPref(SHARED_PREFS_KEY_INT_BLB_SEEN, 0);
        setSharedPref(SHARED_PREFS_KEY_INT_BLB_SHARED, 0);
        setSharedPref(SHARED_PREFS_KEY_FLOAT_BLB_AVG_SEEN_DAY, 0.0);
        setSharedPref(SHARED_PREFS_KEY_INT_GEN_SEEN, 0);
        setSharedPref(SHARED_PREFS_KEY_INT_GEN_SHARED, 0);
        setSharedPref(SHARED_PREFS_KEY_FLOAT_GEN_AVG_SEEN_DAY, 0.0);
        setSharedPref(SHARED_PREFS_KEY_LONG_FIRST_TIME, Calendar.getInstance().getTimeInMillis());

        calcAvgPerDayStats();

        Log.d(LOG_ID, "Stats have been reset!");
        Toast.makeText(this, getString(R.string.stats_reset), Toast.LENGTH_SHORT).show();
    }
}
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
import com.mikkel.tais.imeme.Utils.CapturePhotoUtils;
import com.mikkel.tais.imeme.MainActivity;
import com.mikkel.tais.imeme.Models.Stats;
import com.mikkel.tais.imeme.R;

import java.util.Calendar;
import java.util.Date;

import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_BOOL_FIRST_TIME;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_BLB_SAVED;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_BLB_SEEN;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_INT_BLB_SHARED;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_KEY_STRING_FIRST_TIME;
import static com.mikkel.tais.imeme.Models.Stats.SHARED_PREFS_NAME;

/**
 * This Service is supposed to handle URL calls getting Memes as well as nofitications for the user.
 * The Service should be persistent so it can send notifications when the app is not open.
 */
public class IMemeService extends Service {
    // TODO: Make Service persistent
    // TODO: Make Notifications based on user preferences

    private static final String CHANNEL_ID = "IMemeServiceNotification";
    public static final String BROADCAST_NEW_BILL_MEME_AVAILABLE = "broadcast_new_bill_meme_available";
    public static final String BROADCAST_LIST_OF_MEMES = "broadcast_list_of_memes";
    public static final String BROADCAST_GENERATED_MEME = "broadcast_generated_meme";
    public static final String BROADCAST_GENERATED_MEME_IMG = "broadcast_generated_meme_img";

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
    private int totalBLBSeen;
    private int totalBLBSaved;
    private int totalBLBShared;
    /* Time spend watching memes? Much harder to impl.
        Maybe some time variable which is updated based on some activity lifetime?
        e.g onCreate -> timeStarted. onDestroy updates with timeStarted minus timeNow and save to preferences
    */
    public SharedPreferences prefs;

    // Volley stuff
    private RequestQueue volleyQueue;

    // Notification stuff
    NotificationManagerCompat notificationManager;
    private static final int NOTIFICATION_ID = 101;
    Integer silentTimeStart, silentTimeEnd;
    boolean notificationLevel;
    private static final long NOTIFICATION_DELAY = 1000*60*60; // 60 minutes
    Handler notificationHandler = new Handler();
    Runnable notificationRunnable = new Runnable() {
        @Override
        public void run() {
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

        // Init stuff
        volleyQueue = Volley.newRequestQueue(this);
        notificationManager = NotificationManagerCompat.from(this);
        setupSharedPrefs();
        loadStatsVariables();

        // Load notificationVariables
        loadNotificationVariables();

        // Very important on Android 8.0 and higher to create notificationChannel!
        createNotificationChannel();
        notificationHandler.postDelayed(notificationRunnable, 5);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // # # # Functionality functions # # #
    public Bitmap getRandomMeme() {
        return randomBillMeme;
    }

    private void loadNotificationVariables() {
        // TODO: Get from saved preferences

        silentTimeStart = 0;
        silentTimeEnd = 0;
        notificationLevel = true;
    }

    private void loadStatsVariables() {
        if (prefs.getBoolean(SHARED_PREFS_KEY_BOOL_FIRST_TIME, true)) {
            setSharedPref(SHARED_PREFS_KEY_BOOL_FIRST_TIME, false);
            setSharedPref(SHARED_PREFS_KEY_STRING_FIRST_TIME, Calendar.getInstance().getTime().toLocaleString());
        }

        Stats stats = getStatsFromSP();
        totalBLBSeen = stats.getTotalBLBSeen();
        totalBLBSaved = stats.getTotalBLBSaved();
        totalBLBShared = stats.getTotalBLBShared();
    }


    // REF: Inspiration found on https://android--examples.blogspot.com/2017/02/android-volley-image-request-example.html
    public void requestRandomMeme() {
        final String imageUrl = "https://belikebill.ga/billgen-API.php?default=1";

        ImageRequest imageRequest = new ImageRequest(
                imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        // TODO: Do something with response! Maybe save it locally and send a broadcast with link.
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

    public void saveImageToStorage(Bitmap source, String title) {
        //TODO: Externalizeeeee!
        String url = "Image not saved";

        if (checkPermissionWRITE_EXTERNAL_STORAGE(this)) {
            CapturePhotoUtils.insertImage(getContentResolver(), source, title, "Image generated from iMeme");
            url = "Image saved";

            totalBLBSaved += 1;
            setSharedPref(SHARED_PREFS_KEY_INT_BLB_SAVED, totalBLBSaved);
        }

        Toast.makeText(this, url, Toast.LENGTH_SHORT).show();
    }

    // Permission checker and showDialog from https://stackoverflow.com/questions/37672338/java-lang-securityexception-permission-denial-reading-com-android-providers-me
    private boolean checkPermissionWRITE_EXTERNAL_STORAGE(final Context context) {
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

    private void showDialog(final String msg, final Context context, final String permission) {
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

    // # # # Notifications # # #
    // REF: https://developer.android.com/training/notify-user/build-notification
    private void notifyUserAboutNewMeme() {
        // TODO: I think this will start MainActivity when pressing notification. Should be changed to randomBillMeme later.
        if (notificationLevel && silentTimeNotNow()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_menu_share)
                    .setContentTitle("iMeme")
                    .setContentText("Check out this new meme!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // Following two lines makes you able to tap on the notification to shoot pendingIntent
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    private boolean silentTimeNotNow() {
        Calendar cal = Calendar.getInstance();
        Integer currentHour = cal.get(cal.HOUR_OF_DAY), currentMinute = cal.get(cal.MINUTE);

        Integer silentTimeStartHour = silentTimeStart / 60, silentTimeStartMinute = silentTimeStart % 60;
        Integer silentTimeEndHour = silentTimeEnd / 60, silentTimeEndMinute = silentTimeEnd % 60;

        // All is good. We are outside the silentTime.
        if (currentHour < silentTimeStartHour || currentHour > silentTimeEndHour) {
            return true;
        } else {
            // We are close to startTime / endTime
            if (currentHour.equals(silentTimeStartHour)) {
                if (currentMinute < silentTimeStartMinute) {
                    return true;
                } else {
                    return false;
                }
            } else if (currentHour.equals(silentTimeEndHour)) {
                if (currentMinute > silentTimeEndMinute) {
                    return true;
                } else {
                    return false;
                }
            } else {
                Log.d(LOG_ID, "Error in interpretting sileTimeNotNow()");
                return false;
            }
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // TODO: What is name and description?
            CharSequence name = "name"; //getString(R.string.channel_name);
            String description = "description"; //getString(R.string.channel_description);

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void setNotificationLevel(boolean level){
        notificationLevel = level;
        // TODO: Save to preferences
    }

    public void setSilentTime(Integer silentTimeStart_, Integer silentTimeEnd_){
        silentTimeStart = silentTimeStart_;
        silentTimeEnd = silentTimeEnd_;
        // TODO: Save to preferences
        // TODO: Set alarms
    }

    public boolean getNotificationLevel(){
        return notificationLevel;
    }

    public Integer getSilentTimeStart(){
        return silentTimeStart;
    }

    public Integer getSilentTimeEnd(){
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

        //TODO: update for generated meme
        //totalBLBSeen += 1;
        //setSharedPref(SHARED_PREFS_KEY_INT_BLB_SEEN, totalBLBSeen);
    }

    // # # # Functions for StatsActivity # # #
    private void setupSharedPrefs() {
        prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
    }

    public Stats getStatsFromSP() {
        Stats stats = new Stats();

        stats.setTotalBLBSeen(prefs.getInt(SHARED_PREFS_KEY_INT_BLB_SEEN, 0));
        stats.setTotalBLBSaved(prefs.getInt(SHARED_PREFS_KEY_INT_BLB_SAVED, 0));
        stats.setTotalBLBSaved(prefs.getInt(SHARED_PREFS_KEY_INT_BLB_SHARED, 0));

        return stats;
    }

    public String getFirstUsageFromSP() {
        return prefs.getString(SHARED_PREFS_KEY_STRING_FIRST_TIME, null);
    }

    public void setSharedPref(String key, Object value) {
        SharedPreferences.Editor editor = prefs.edit();

        if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }

        editor.apply();
    }

    public void resetStats(){
        // TODO: impl.
    }
}
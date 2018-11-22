package com.mikkel.tais.imeme.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.mikkel.tais.imeme.MainActivity;
import com.mikkel.tais.imeme.R;

/**
 * This Service is supposed to handle URL calls getting Memes as well as nofitications for the user.
 * The Service should be persistent so it can send notifications when the app is not open.
 */
public class IMemeService extends Service {

    private static final String CHANNEL_ID = "IMemeServiceNotification";
    public static final String BROADCAST_RESULT = "broadcast_result";
    public static final String BROADCAST_NEW_BILL_MEME_AVAILABLE = "broadcast_new_bill_meme_available";
    private final IBinder binder = new IMemeUpdateServiceBinder();
    private static final String LOG_ID = "iMemeService_log";
    private Bitmap randomBillMeme;


    // Volley stuff
    private RequestQueue volleyQueue;// = Volley.newRequestQueue(this);

    // Notification stuff
    NotificationManagerCompat notificationManager;// = NotificationManagerCompat.from(this);
    private static final int NOTIFICATION_ID = 101;

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

        // Very important on Android 8.0 and higher to create notificationChannel!
        createNotificationChannel();
        notifyUserAboutNewMeme();

        // For testing
        //getRandomMeme();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    // # # # Functionality functions # # #

    public Bitmap getRandomMeme(){
        return randomBillMeme;
    }

    public void requestRandomMeme(){
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
                        randomBillMeme = response;
                        broadcastNewBillMemeAvailable("");
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

    // # # # Notifications # # #

    // REF: https://developer.android.com/training/notify-user/build-notification
    private void notifyUserAboutNewMeme() {
        // TODO: I think this will start MainActivity when pressing notification. Should be changed to randomBillMeme later.
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

    // # # # BROADCAST # # #
    private void broadcastNewBillMemeAvailable(String result){
        Log.d(LOG_ID, "Broadcasting new bill meme available.");
        Intent intent = new Intent(BROADCAST_NEW_BILL_MEME_AVAILABLE);
        intent.putExtra(BROADCAST_RESULT, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void registerBroadcast(BroadcastReceiver broadcastDataUpdatedReceiver){
        Log.d(LOG_ID, "registering receivers");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_NEW_BILL_MEME_AVAILABLE);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastDataUpdatedReceiver, filter);
    }

    public void unRegisterBroadcast(BroadcastReceiver broadcastDataUpdatedReceiver){
        Log.d(LOG_ID, "unregistering receivers");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastDataUpdatedReceiver);
    }
}

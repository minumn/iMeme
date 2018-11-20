package com.mikkel.tais.imeme;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * This Service is supposed to handle URL calls getting Memes as well as nofitications for the user.
 * The Service should be persistent so it can send notifications when the app is not open.
 */
public class IMemeService extends Service {

    private final IBinder binder = new IMemeUpdateServiceBinder();
    private static final String LOG_ID = "iMemeService_log";


    // Volley stuff
    private RequestQueue volleyQueue = Volley.newRequestQueue(this);

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

        // For testing
        getRandomMeme();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    // # # # Functionality functions # # #

    public void getRandomMeme(){
        final String imageUrl = "https://belikebill.ga/billgen-API.php?default=1";

        // TODO: Volley not tested!
        // REF: Inspiration found on https://android--examples.blogspot.com/2017/02/android-volley-image-request-example.html
        // TODO: Could look at above link how they save files.

        ImageRequest imageRequest = new ImageRequest(
                imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        // TODO: Do something with response! Maybe save it locally and send a broadcast with link.

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

}

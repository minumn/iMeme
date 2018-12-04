package com.mikkel.tais.imeme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.mikkel.tais.imeme.Fragments.BlankFragment;
import com.mikkel.tais.imeme.Fragments.MemeGeneratorFragment;
import com.mikkel.tais.imeme.Fragments.RandomMemeFragment;
import com.mikkel.tais.imeme.Fragments.StatsFragment;
import com.mikkel.tais.imeme.Services.IMemeService;

// REF: This class have been made using the default of Android Studio and previous assignments of ours.
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String LOG_ID = "MainActivity_log";

    // Stuff for IMeme Service
    public IMemeService iMemeService;
    private ServiceConnection serviceConnection;
    private boolean boundToIMemeService = false;

    // Fragment stuff
    private Fragment currentFragment;
    private RandomMemeFragment randomMemeFragment = new RandomMemeFragment();
    private StatsFragment statsFragment = new StatsFragment();
    private MemeGeneratorFragment memeGeneratorFragment = new MemeGeneratorFragment();
    private BlankFragment mainFragment = new BlankFragment();
    private static final int SETTINGS_REQ = 102;

    // # # # SERVICE FUNCTIONALITY # # #
    private void startIMemeService() {
        startService(new Intent(MainActivity.this, IMemeService.class));
    }

    private void setupConnectionToIMemeService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                iMemeService = ((IMemeService.IMemeUpdateServiceBinder) service).getService();
                Log.d(LOG_ID, "iMeme service connected.");
            }

            public void onServiceDisconnected(ComponentName className) {
                iMemeService = null;
                Log.d(LOG_ID, "iMeme service disconnected.");
            }
        };
    }

    private void bindToIMemeService() {
        Intent intent = new Intent(MainActivity.this, IMemeService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        boundToIMemeService = true;
    }

    private void unBindFromIMemeService() {
        if (boundToIMemeService) {
            unbindService(serviceConnection);
            boundToIMemeService = false;
        }
    }

    // # # # UTILITY FUNCTIONS # # #

    private void setFragmentView(Fragment fragment) {
        if (findViewById(R.id.fragment_container) != null && currentFragment != fragment) {

            FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment);

            fragmentTransaction.addToBackStack(null).commit();
        }
    }

    private void initiateDrawerMenu() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setMainFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, mainFragment)
                .commit();
    }

    // # # # onFunctions # # #
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make sure Service is running.
        startIMemeService();
        setupConnectionToIMemeService();
        bindToIMemeService();

        initiateDrawerMenu();

        if (savedInstanceState == null) {
            setMainFragment();
        }

        String menuFragment = getIntent().getAction();

        if (menuFragment != null && !menuFragment.equals("android.intent.action.MAIN")) {
            if (menuFragment.equals("bill")) {
                setFragmentView(randomMemeFragment);
            } else {
                Toast.makeText(this, menuFragment, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unBindFromIMemeService();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            currentFragment = null;
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), SETTINGS_REQ);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //REF: https://stackoverflow.com/questions/32359727/method-to-refresh-fragment-content-when-data-changed-like-recall-oncreateview?lq=1
        if (requestCode == SETTINGS_REQ && currentFragment == statsFragment) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.detach(statsFragment);
            fragmentTransaction.attach(statsFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_randomBillMeme:
                setFragmentView(randomMemeFragment);
                currentFragment = randomMemeFragment;
                break;
            case R.id.nav_memeGen:
                setFragmentView(memeGeneratorFragment);
                currentFragment = memeGeneratorFragment;
                break;
            case R.id.nav_gallery:
                Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivity(intent);
                break;
            case R.id.nav_stats:
                setFragmentView(statsFragment);
                currentFragment = statsFragment;
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }
}
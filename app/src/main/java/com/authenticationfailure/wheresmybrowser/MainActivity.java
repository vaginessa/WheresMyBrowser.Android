package com.authenticationfailure.wheresmybrowser;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Button goButton;
    WebView webview;
    EditText urlBar;
    WebViewPreferencesManager webViewPreferencesManager;
    Editor settingsEditor;
    ProgressBar webViewProgressBar;
    int REQUEST_SD_WRITE_PERMISSION = 1;

    static String DEFAULT_URL = "http://www.example.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                //Hide keyboard on drawer opening
                InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        goButton = findViewById(R.id.goButton);
        webview = findViewById(R.id.webView);
        urlBar = findViewById(R.id.urlText);
        webViewProgressBar = findViewById(R.id.webViewProgressBar);

        // Learn about web views here:
        // https://developer.android.com/guide/webapps/webview.html

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadURLFromBar();
            }
        });

        /*
        *  webview settings are managed by the WebViewPreferencesManager
        *  go check the code
        */
        webViewPreferencesManager = new WebViewPreferencesManager(webview, getApplicationContext());

        settingsEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        requestExternalStoragePrivileges();

        setupData();

        setupUrlBar();

        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress == 100) { newProgress = 0; };
                webViewProgressBar.setProgress(newProgress);
            }
        });
    }

    private void requestExternalStoragePrivileges() {
        // Ask for external SD card permission for Android 6.0 API 23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int storagePermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (storagePermission != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_SD_WRITE_PERMISSION);
            }
        }
    }

    private void setupData() {
        SecretDatabaseHelper secretDb = new SecretDatabaseHelper(getApplicationContext());
        Cursor cursor = secretDb.getReadableDatabase().query("SECRET_TABLE",
                new String[]{"id","secret"},
                null,
                null,
                null,
                null,
                null);
        cursor.moveToFirst();
        Log.i("SecretDatabase", cursor.getString(cursor.getColumnIndex("secret")));

        File file;
        try {
            file = new File(getFilesDir().getCanonicalPath().concat("test.txt"));
            if (file.exists()) {
                Log.i("FILE_CREATE","File" + file.getAbsolutePath() + " already exists");
            } else {
                FileOutputStream fOut = new FileOutputStream(file);
                fOut.write("Hello".getBytes());
                fOut.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("FILE_CREATE", "Error creating file");
        }
    }

    private void loadURLFromBar() {
        webview.loadUrl(urlBar.getText().toString());
    }

    private void setupUrlBar() {
        urlBar.setText(DEFAULT_URL);

        TextView.OnEditorActionListener urlBarEditorListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_NULL
                        && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    loadURLFromBar();
                }
                return false;
            }
        };

        urlBar.setOnEditorActionListener(urlBarEditorListener);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
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

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        //InputMethodManager inputMethodManager = (InputMethodManager)  this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
        } else if (id == R.id.nav_documentation) {
            // TODO: update the URL after uploading the documentation
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.authenticationfailure.com"));
            startActivity(intent);

        } else if (id == R.id.nav_scenario_1) {

            // TODO: setup scenario
            settingsEditor.putBoolean("enable_javascript", true);
            settingsEditor.putBoolean("enable_webview_client", true);
            settingsEditor.putBoolean("enable_webview_debugging", true);
            settingsEditor.putBoolean("enable_file_access", true);
            settingsEditor.putBoolean("enable_file_access_from_file_url", true);
            settingsEditor.putBoolean("enable_universal_access_from_file_url", true);
            settingsEditor.putBoolean("enable_javascript_interface", false);
            settingsEditor.commit();

            urlBar.setText("https://www.authenticationfailure.com/");
            loadURLFromBar();

        } else if (id == R.id.nav_scenario_2) {

            // TODO: setup scenario
            settingsEditor.putBoolean("enable_javascript", true);
            settingsEditor.putBoolean("enable_webview_client", true);
            settingsEditor.putBoolean("enable_webview_debugging", true);
            settingsEditor.putBoolean("enable_file_access", false);
            settingsEditor.putBoolean("enable_file_access_from_file_url", false);
            settingsEditor.putBoolean("enable_universal_access_from_file_url", false);
            settingsEditor.putBoolean("enable_javascript_interface", true);
            settingsEditor.commit();

            urlBar.setText("https://www.authenticationfailure.com/");
            loadURLFromBar();

        } else if (id == R.id.nav_scenario_3) {

            // TODO: setup scenario

        } else if (id == R.id.nav_share) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
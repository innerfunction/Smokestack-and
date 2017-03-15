package com.innerfunction.smokestack;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import com.innerfunction.scffld.app.ManifestMetaData;

/**
 * Base application class for Smokestack based apps.
 *
 * Created by juliangoacher on 15/03/2017.
 */
public class Application extends android.app.Application {

    static final String Tag = Application.class.getSimpleName();

    static final boolean TraceEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ManifestMetaData.applyTo( this );
        try {
            // Enable debugging of webviews via titleBarState.
            // Taken from https://developer.chrome.com/devtools/docs/remote-debugging#debugging-webviews
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
                if( 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE) ) {
                    WebView.setWebContentsDebuggingEnabled( true );
                }
            }
            if( TraceEnabled) {
                android.os.Debug.startMethodTracing("smokestack-trace");
            }
            // Configure and start the app container.
            AppContainer appContainer = AppContainer.getAppContainer( getApplicationContext() );
            if( TraceEnabled ) {
                android.os.Debug.stopMethodTracing();
            }
            appContainer.startService();
        }
        catch(Exception e) {
            Log.e(Tag, "Application startup failure", e );
        }
    }
}

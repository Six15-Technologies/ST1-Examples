// Copyright 2021 Six15 Technologies
//
// Redistribution and use in source and binary forms, with or without modification,
// are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
//    products derived from this software without specific prior written permission.
// 4. This software, with or without modification, must only be used with the copyright holder’s hardware.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO,THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
// IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
// OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
// EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.six15.examples_test.view_rendering;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudService;
import com.six15.examples.helpers.HudViewRenderingHelper;
import com.six15.examples_test.R;
import com.six15.examples_test.ServiceIDs;
import com.six15.hudservice.IHudService;

public class BackgroundViewRenderingService extends HudService {
    private static final String TAG = BackgroundViewRenderingService.class.getSimpleName();

    public static final String EXTRA_SHOULD_RUN = "EXTRA_SHOULD_RUN";
    private static final String NOTIFICATION_CHANNEL_DEFAULT = TAG + ".NOTIFICATION_CHANNEL_DEFAULT";
    private IHudService mHmdService;
    private HudViewRenderingHelper mViewRenderingHelper;
    private boolean mConnected = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        int result = START_NOT_STICKY;
        if (intent == null) {
            return result;
        }
        boolean shouldRun = intent.getBooleanExtra(EXTRA_SHOULD_RUN, false);
        Log.i(TAG, "onStartCommand: shouldRun:" + shouldRun);
        if (shouldRun) {
            startInForeground();
            startShowingView();
        } else {
            stopShowingView();
            stopSelf(startId);
        }
        return result;
    }

    private void startInForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DEFAULT, TAG, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }
        PendingIntent stopServiceIntent = PendingIntent.getService(this, 0, new Intent(this, getClass()), PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_DEFAULT)
                .setContentTitle("Background View Rendering Service")
                .setSmallIcon(R.drawable.ic_baseline_stop_circle_24)
                .setTicker("Running in Background")
                .addAction(R.drawable.ic_baseline_stop_circle_24, "Stop Service", stopServiceIntent)
                .build();

        startForeground(ServiceIDs.BACKGROUND_VIEW_RENDERING_SERVICE, notification);
    }

    @Override
    protected @NonNull HudCallbacks getCallbacks() {
        return new HudCallbacks() {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
            }

            @Override
            public void onConnected(boolean connected) throws RemoteException {
                super.onConnected(connected);
                mConnected = connected;
                if (mViewRenderingHelper != null) {
                    //Turn on/off drawing based on device connection.
                    mViewRenderingHelper.setAutoDraw(mConnected, mHmdService);
                }
            }
        };
    }

    private void startShowingView() {
        if (mViewRenderingHelper != null) {
            return;
        }
        mViewRenderingHelper = new HudViewRenderingHelper(this, R.layout.hud_webview, R.style.Theme_HUD, null);
        View rootView = mViewRenderingHelper.getView();
        WebView webView = rootView.findViewById(R.id.hud_webview_web);
        webView.setInitialScale(100);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://time.is/");

        if (mConnected && mViewRenderingHelper != null) {
            mViewRenderingHelper.setAutoDraw(mConnected, mHmdService);
        }
    }

    private void stopShowingView() {
        if (mViewRenderingHelper != null) {
            mViewRenderingHelper.setAutoDraw(false, null);
        }
        mViewRenderingHelper = null;
        if (mHmdService != null){
            try {
                mHmdService.clearHudDisplay();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}

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
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudService;
import com.six15.examples.helpers.HudSurfaceViewRenderingHelper;
import com.six15.examples_test.R;
import com.six15.examples_test.ServiceIDs;
import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.IHudService;

public class BackgroundSurfaceViewRenderingService extends HudService {
    private static final String TAG = BackgroundSurfaceViewRenderingService.class.getSimpleName();

    public static final String EXTRA_SHOULD_RUN = "EXTRA_SHOULD_RUN";
    private static final String NOTIFICATION_CHANNEL_DEFAULT = TAG + ".NOTIFICATION_CHANNEL_DEFAULT";
    private IHudService mHmdService;
    private HudSurfaceViewRenderingHelper mSurfaceViewRenderingHelper;
    private boolean mConnected = false;
    private MediaPlayer mMediaPlayer;
    private int mCounter = 0;
    private TextView mTextView;

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
                .setContentTitle("Background Surface View Rendering Service")
                .setSmallIcon(R.drawable.ic_baseline_stop_circle_24)
                .setTicker("Running in Background")
                .addAction(R.drawable.ic_baseline_stop_circle_24, "Stop Service", stopServiceIntent)
                .build();

        startForeground(ServiceIDs.BACKGROUND_SURFACEVIEW_RENDERING_SERVICE, notification);
    }

    @Override
    protected @NonNull
    HudCallbacks getCallbacks() {
        return new HudCallbacks(Looper.getMainLooper()) {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
            }

            @Override
            public void onConnected(boolean connected) throws RemoteException {
                super.onConnected(connected);
                mConnected = connected;
                if (mSurfaceViewRenderingHelper != null) {
                    //Turn on/off drawing based on device connection.
                    Log.i(TAG, "onConnected: width:" + mSurfaceViewRenderingHelper.getView().getWidth());
                    mSurfaceViewRenderingHelper.startDrawing(mHmdService);
                }
            }
        };
    }

    private boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private void startShowingView() {
        if (mSurfaceViewRenderingHelper != null) {
            return;
        }
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Can't start without overlay permissions", Toast.LENGTH_SHORT).show();
        }
        mCounter = 0;
        boolean useOverlay = true;
        //By using an overlay, we need to worry about "displaying over other applications".
        //For this, our SurfaceViews will work from the background.
        mSurfaceViewRenderingHelper = new HudSurfaceViewRenderingHelper(this, R.layout.hud_surfaceview, R.style.Theme_HUD, useOverlay, new HudSurfaceViewRenderingHelper.Callbacks() {
            @Override
            public void onDraw(Bitmap bitmap, ByteFrame jpegBytes) {
                mCounter++;
                mTextView.setText(getString(R.string.counter_format, mCounter));
            }
        });
        //There is likely no reason for the image scale to change from 1.
        //HudViewSurfaceRenderingHelper has an internal DPI that could be adjusted if desired.
//        mSurfaceViewRenderingHelper.setImageScale(1);
        View rootView = mSurfaceViewRenderingHelper.getView();
        mTextView = rootView.findViewById(R.id.hud_surfaceview_text);
        mTextView.setText(getString(R.string.counter_format, mCounter));
        SurfaceView surfaceView = rootView.findViewById(R.id.hud_surfaceview_text_surface);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.example_video);
                mMediaPlayer.setDisplay(holder);
                mMediaPlayer.start();
                mMediaPlayer.setLooping(true);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
        if (mConnected && mSurfaceViewRenderingHelper != null) {
            mSurfaceViewRenderingHelper.startDrawing(mHmdService);
        }
    }

    private void stopShowingView() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mSurfaceViewRenderingHelper != null) {
            mSurfaceViewRenderingHelper.stopDrawing(null);
            mSurfaceViewRenderingHelper = null;
        }
        if (mHmdService != null) {
            try {
                mHmdService.clearHudDisplay();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}

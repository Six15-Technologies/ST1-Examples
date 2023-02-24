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

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudCompatActivity;
import com.six15.examples.helpers.HudSurfaceViewRenderingHelper;
import com.six15.examples_test.R;
import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.IHudService;

public class ForegroundSurfaceViewRenderingActivity extends HudCompatActivity {
    private static final String TAG = ForegroundSurfaceViewRenderingActivity.class.getSimpleName();
    private IHudService mHmdService;
    private MediaPlayer mMediaPlayer;
    private TextView mTextView;
    @Nullable
    private HudSurfaceViewRenderingHelper mHelper;
    private int mCounter = 0;
    private boolean mIsStarted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);
    }

    private void handleStart() {
        Log.d(TAG, "handleStart() called");
        if (mHmdService == null || !mIsStarted) {
            Log.e(TAG, "handleStart: Not ready to start!");
            return;
        }
        if (mHelper != null) {
            Log.i(TAG, "handleStart: Already running");
            //We're already running
            return;
        }
        mCounter = 0;
        boolean useOverlay = false;
        //By not using an overlay, we don't need to worry about "displaying over other applications".
        //However, this means any SurfaceView's wil only work while our activity is in the foreground.
        //This means they will not work while the screen is off or the activity is stopped.
        //Using an overlay is best done from a Foreground Service as the Foreground Service lifecycle better matches what we're trying to do.
        mHelper = new HudSurfaceViewRenderingHelper(this, R.layout.hud_surfaceview, R.style.Theme_HUD, useOverlay, new HudSurfaceViewRenderingHelper.Callbacks() {
            @Override
            public void onDraw(Bitmap bitmap, ByteFrame jpegBytes) {
                mCounter++;
                if (mTextView != null) {
                    mTextView.setText(getString(R.string.counter_format, mCounter));
                }
            }
        });
        View rootView = mHelper.getView();
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
        mHelper.startDrawing(mHmdService);
    }

    protected void handleStop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mHelper != null) {
            mHelper.stopDrawing(mHmdService);
            mHelper = null;
            mTextView = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsStarted = true;
        if (mHmdService != null) {
            handleStart();
        }
    }

    @Override
    protected void onStop() {
        mIsStarted = false;
        handleStop();
        super.onStop();
        mHmdService = null;
    }

    @Override
    protected void onDestroy() {
        mIsStarted = false;
        handleStop();
        super.onDestroy();
        mHmdService = null;
    }

    @NonNull
    @Override
    protected HudCallbacks getCallbacks() {
        return new HudCallbacks(Looper.getMainLooper()) {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
                if (mIsStarted && mHmdService != null) {
                    handleStart();
                }
            }
        };
    }
}

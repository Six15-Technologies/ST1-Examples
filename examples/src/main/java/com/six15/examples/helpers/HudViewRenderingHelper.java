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

package com.six15.examples.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.Constants;
import com.six15.hudservice.IHudService;

import java.io.ByteArrayOutputStream;

public class HudViewRenderingHelper {

    private static final String TAG = HudViewRenderingHelper.class.getSimpleName();
    private final View mView;
    private Canvas mDrawingCanvas;
    private Bitmap mDrawingBitmap;
    private final Handler mHandler;
    private final LayoutInflater mLayoutInflater;
    private final Callbacks mCallbacks;
    private boolean mAutoDrawing;
    private Runnable mDrawingRunnable;
    private final ByteArrayOutputStream mByteOutput;
    private final ByteFrame mJpegFrame;
    //This is a bit slower than 30fps. Less frames are dropped slightly slower which looks smoother.
    private static final int DESIRED_FRAME_TIME_MS = 35;
    private int mJpegQuality = 95;
    private int mImageScale = 1;

    public HudViewRenderingHelper(Context context, @LayoutRes int layout, @StyleRes int theme, @Nullable Callbacks callback) {
        mCallbacks = callback;
        setupDrawing(mImageScale);
        mJpegFrame = new ByteFrame();
        mByteOutput = new ByteArrayOutputStream();
        mHandler = new Handler(Looper.getMainLooper());

        Configuration hudConfig = new Configuration(context.getResources().getConfiguration());
        hudConfig.densityDpi = 200;
        hudConfig.touchscreen = Configuration.TOUCHSCREEN_NOTOUCH;
        hudConfig.orientation = Configuration.ORIENTATION_LANDSCAPE;
        hudConfig.fontScale = 1.0f;//Don't allow system font scale to change hud text size.
        Context hudContext = context.createConfigurationContext(hudConfig);
        hudContext.setTheme(theme);
        mLayoutInflater = LayoutInflater.from(hudContext);

        mView = mLayoutInflater.inflate(layout, null, false);
        triggerLayout();
    }

    public void setQuality(int jpegQuality) {
        mJpegQuality = jpegQuality;
    }

    private void setupDrawing(int scale) {
        mImageScale = scale;
        mDrawingBitmap = Bitmap.createBitmap(Constants.ST1_HUD_WIDTH * mImageScale, Constants.ST1_HUD_HEIGHT * mImageScale, Bitmap.Config.ARGB_8888);
        mDrawingBitmap.eraseColor(Color.BLACK);
        mDrawingCanvas = new Canvas(mDrawingBitmap);
    }

    public void setImageScale(int scale) {
        setupDrawing(scale);
        triggerLayout();
    }

    public interface Callbacks {
        void onDraw(Bitmap bitmap, ByteFrame jpegBytes);
    }

    public void triggerLayout() {
        int width = Constants.ST1_HUD_WIDTH * mImageScale;
        int height = Constants.ST1_HUD_HEIGHT * mImageScale;
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        mView.measure(measuredWidth, measuredHeight);
        mView.layout(0, 0, width, height);
    }

    public View inflateView(@LayoutRes int layout, ViewGroup parent, boolean attachToRoot) {
        return mLayoutInflater.inflate(layout, parent, attachToRoot);
    }

    public View getView() {
        return mView;
    }

    public ByteFrame getLastFrame() {
        return mJpegFrame;
    }

    public void draw(IHudService hudService) {
        if (!mAutoDrawing) {
            drawInternal(hudService);
        }
    }

    protected void drawInternal(IHudService hudService) {
        mDrawingBitmap.eraseColor(Color.BLACK);
        mView.draw(mDrawingCanvas);
        boolean recycleAfter;
        Bitmap hudBitmap;
        if (mImageScale != 1) {
            hudBitmap = Bitmap.createScaledBitmap(mDrawingBitmap, Constants.ST1_HUD_WIDTH, Constants.ST1_HUD_HEIGHT, true);
            recycleAfter = true;
        } else {
            hudBitmap = mDrawingBitmap;
            recycleAfter = false;
        }
        hudBitmap.compress(Bitmap.CompressFormat.JPEG, mJpegQuality, mByteOutput);
        mJpegFrame.set_byte(mByteOutput.toByteArray());
        mByteOutput.reset();
        if (hudService != null) {
            try {
                hudService.sendBufferToHud(mJpegFrame);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (mCallbacks != null) {
            mCallbacks.onDraw(hudBitmap, mJpegFrame);
        }
        if (recycleAfter) {
            hudBitmap.recycle();
        }

    }

    public void setAutoDraw(boolean autoDraw, IHudService hudService) {
        if (autoDraw == mAutoDrawing) {
            return;
        }
        mAutoDrawing = autoDraw;
        if (mAutoDrawing) {
            if (mDrawingRunnable != null) {
                mHandler.removeCallbacks(mDrawingRunnable);
            }
            mDrawingRunnable = new Runnable() {

                @Override
                public void run() {
                    long timeStart = System.nanoTime() / 1000000L;
                    mHandler.removeCallbacks(this);
                    if (mAutoDrawing) {
                        drawInternal(hudService);
                        long timeNow = System.nanoTime() / 1000000L;
                        long timeDiff = timeNow - timeStart;
//                        Log.d(TAG, "run: timeDiff:" + timeDiff);
                        long delay = Math.min(Math.max(DESIRED_FRAME_TIME_MS - timeDiff, 0), DESIRED_FRAME_TIME_MS);
//                        Log.d(TAG, "run() using delay:" + delay);
                        mHandler.postDelayed(this, delay);
                    }
                }
            };
            mHandler.post(mDrawingRunnable);
        }
    }
}

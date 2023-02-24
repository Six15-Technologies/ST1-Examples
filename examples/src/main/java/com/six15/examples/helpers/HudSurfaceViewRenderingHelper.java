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
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.Constants;
import com.six15.hudservice.IHudService;

import java.io.ByteArrayOutputStream;

public class HudSurfaceViewRenderingHelper {
    private static final String TAG = HudSurfaceViewRenderingHelper.class.getSimpleName();
    private static final boolean DEBUG = false;

    private final View mView;
    private final WindowManager mWindowManager;
    private final LayeredPixelCopy mLayeredPixelCopy;
    private final Paint mPaint;
    private final boolean mUseOverlay;
    private Canvas mDrawingCanvas;
    private Canvas mDrawingSurfacesCanvas;
    private Bitmap mDrawingBitmap;
    private Bitmap mDrawingSurfacesBitmap;
    private final Handler mHandler;
    private final LayoutInflater mLayoutInflater;
    private final Callbacks mCallbacks;
    private boolean mAutoDrawing = false;
    private Runnable mDrawingRunnable;
    private final ByteArrayOutputStream mByteOutput;
    private final ByteFrame mJpegFrame;
    //This is a bit slower than 30fps. Less frames are dropped slightly slower which looks smoother.
    private static final int DESIRED_FRAME_TIME_MS = 35;
    private int mJpegQuality = 95;
    private int mImageScale = 1;

    //https://levelup.gitconnected.com/add-view-outside-activity-through-windowmanager-1a70590bad40


    public HudSurfaceViewRenderingHelper(Context context, @LayoutRes int layout, @StyleRes int theme, boolean useOverlay, @Nullable Callbacks callback) {
        mCallbacks = callback;
        setupDrawing();
        mJpegFrame = new ByteFrame();
        mPaint = new Paint();
        mByteOutput = new ByteArrayOutputStream();
        mHandler = new Handler(Looper.getMainLooper());
        mUseOverlay = useOverlay;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);


        Configuration hudConfig = new Configuration(context.getResources().getConfiguration());
        hudConfig.densityDpi = 200;
        hudConfig.touchscreen = Configuration.TOUCHSCREEN_NOTOUCH;
        hudConfig.orientation = Configuration.ORIENTATION_LANDSCAPE;
        hudConfig.fontScale = 1.0f;//Don't allow system font scale to change hud text size.
        Context hudContext = context.createConfigurationContext(hudConfig);
        hudContext.setTheme(theme);
        mLayoutInflater = LayoutInflater.from(hudContext);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mLayeredPixelCopy = new LayeredPixelCopy(true);
        } else {
            //On older Android versions we can't grab SurfaceViews so LayeredPixelCopy doesn't work.
            //On those devices this class will simply not render the surface.
            //If surfaces aren't needed at all, the HudViewRenderingHelper might be a better fit.
            mLayeredPixelCopy = null;
        }

        mView = mLayoutInflater.inflate(layout, null, false);
    }

    public void setQuality(int jpegQuality) {
        mJpegQuality = jpegQuality;
    }

    private void setupDrawing() {
        int width = Constants.ST1_HUD_WIDTH * mImageScale;
        int height = Constants.ST1_HUD_HEIGHT * mImageScale;
        mDrawingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mDrawingBitmap.eraseColor(Color.BLACK);
        mDrawingCanvas = new Canvas(mDrawingBitmap);

        mDrawingSurfacesBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mDrawingSurfacesBitmap.eraseColor(Color.BLACK);
        mDrawingSurfacesCanvas = new Canvas(mDrawingSurfacesBitmap);
    }

    // A larger image scale causes the UI to be rendered at a
    public void setImageScale(int scale) {
        mImageScale = scale;
        setupDrawing();
        ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
        layoutParams.width = Constants.ST1_HUD_WIDTH * mImageScale;
        layoutParams.height = Constants.ST1_HUD_HEIGHT * mImageScale;
        mWindowManager.removeViewImmediate(mView);
        mWindowManager.addView(mView, layoutParams);
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

    interface OnComplete {
        void onComplete();
    }

    private void drawInternal(@Nullable IHudService hudService, @NonNull OnComplete callback) {
        mDrawingBitmap.eraseColor(Color.BLACK);
        mDrawingSurfacesBitmap.eraseColor(Color.BLACK);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Draw SurfaceViews
            //This can't be done on older Android versions.
            mLayeredPixelCopy.request(mView, mDrawingSurfacesBitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                @Override
                public void onPixelCopyFinished(int copyResult) {
                    mView.draw(mDrawingCanvas);
                    mDrawingSurfacesCanvas.drawBitmap(mDrawingBitmap, 0, 0, mPaint);
                    continueDrawInternal(hudService, callback, mDrawingSurfacesBitmap);
                }
            }, mHandler);
        } else {
            mView.draw(mDrawingCanvas);
            continueDrawInternal(hudService, callback, mDrawingBitmap);
        }
    }

    private void continueDrawInternal(@Nullable IHudService hudService, OnComplete callback, Bitmap result) {
        boolean recycleAfter;
        Bitmap hudBitmap;
        if (!mAutoDrawing) {
            callback.onComplete();
            return;
        }
        if (mImageScale != 1) {
            hudBitmap = Bitmap.createScaledBitmap(result, Constants.ST1_HUD_WIDTH, Constants.ST1_HUD_HEIGHT, true);
            recycleAfter = true;
        } else {
            hudBitmap = result;
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
        if (callback != null) {
            callback.onComplete();
        }

    }

    public void stopDrawing(@Nullable IHudService hudService) {
        mAutoDrawing = false;
        if (mDrawingRunnable != null) {
            mHandler.removeCallbacks(mDrawingRunnable);
        }
        mWindowManager.removeViewImmediate(mView);
        if (hudService != null) {
            try {
                hudService.clearHudDisplay();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void startDrawing(@Nullable IHudService hudService) {
        if (mAutoDrawing) {
            return;
        }
        mAutoDrawing = true;
        int type;
        if (mUseOverlay) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        if (!DEBUG) {
            layoutParams.alpha = 0;
        }
        layoutParams.width = Constants.ST1_HUD_WIDTH;
        layoutParams.height = Constants.ST1_HUD_HEIGHT;
        mWindowManager.addView(mView, layoutParams);
        triggerLayout();

        mDrawingRunnable = new Runnable() {

            @Override
            public void run() {
                long timeStart = System.nanoTime() / 1000000L;
                mHandler.removeCallbacks(this);
                if (mAutoDrawing) {
                    final Runnable this_but_works = this;//You would think "Runnable.this" would work below, but it doesn't.
                    OnComplete callback = new OnComplete() {
                        @Override
                        public void onComplete() {
                            if (!mAutoDrawing) {
                                return;
                            }
                            long timeNow = System.nanoTime() / 1000000L;
                            long timeDiff = timeNow - timeStart;
//                        Log.d(TAG, "run: timeDiff:" + timeDiff);
                            long delay = Math.min(Math.max(DESIRED_FRAME_TIME_MS - timeDiff, 0), DESIRED_FRAME_TIME_MS);
//                        Log.d(TAG, "run() using delay:" + delay);
                            mHandler.postDelayed(this_but_works, delay);
                        }
                    };
                    boolean hudConnected = false;
                    try {
                        hudConnected = hudService != null && hudService.getHudConnected();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    if (hudConnected) {
                        drawInternal(hudService, callback);
                    } else {
                        callback.onComplete();
                    }
                }
            }
        };
        mHandler.post(mDrawingRunnable);
    }
}

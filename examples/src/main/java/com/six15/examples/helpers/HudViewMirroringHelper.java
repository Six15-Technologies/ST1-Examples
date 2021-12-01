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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import com.six15.examples.HudWhiteTextCanvas;
import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudServiceConnection;
import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.EnumHudMode;
import com.six15.hudservice.IHudService;

import java.io.ByteArrayOutputStream;

public class HudViewMirroringHelper {
    private static final String TAG = HudViewMirroringHelper.class.getSimpleName();
    private final boolean mUseCorrectionCanvas;
    private final View mView;
    private final Paint mDrawingPaint;
    private final boolean mUsePixelCopy;
    private Bitmap mDrawingBitmap;
    private final ByteFrame mJpegFrame;
    private final ByteArrayOutputStream mByteOutput;
    private IHudService mHmdService;
    private final HudServiceConnection mHudConnection;
    private Handler mBackgroundHandler;
    private HandlerThread mHandlerThread;
    private final Handler mMainHandler;
    private boolean mAllowRendering = false;
    private boolean mIsEnabled = false;
    private Canvas mDrawingCanvas;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static HudViewMirroringHelper usingPixelCopy(View view) {
        return new HudViewMirroringHelper(view, true, false);
    }

    public static HudViewMirroringHelper usingDraw(View view, boolean tryToFixTextColor) {
        return new HudViewMirroringHelper(view, false, tryToFixTextColor);
    }

    private HudViewMirroringHelper(@NonNull View view, boolean usePixelCopy, boolean fixColors) {
        mUsePixelCopy = usePixelCopy;
        mUseCorrectionCanvas = fixColors;
        mView = view;
        mHudConnection = new HudServiceConnection(view.getContext(), mCallbacks);
        mJpegFrame = new ByteFrame();
        mByteOutput = new ByteArrayOutputStream();
        mMainHandler = new Handler(Looper.getMainLooper());
        mDrawingPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        view.addOnAttachStateChangeListener(mOnAttachStateChangeListener);
    }

    private final View.OnAttachStateChangeListener mOnAttachStateChangeListener = new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View v) {
            mHandlerThread = new HandlerThread(TAG);
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
            mHudConnection.connectToService();
            v.getViewTreeObserver().addOnDrawListener(onDrawListener);
            ViewTreeLifecycleOwner.get(v).getLifecycle().addObserver(mLifecycleObserver);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            v.getViewTreeObserver().removeOnDrawListener(onDrawListener);
            if (mHandlerThread != null) {
                mHandlerThread.quitSafely();
                mHandlerThread = null;
                mBackgroundHandler = null;
            }
            mHudConnection.disconnectFromService();
        }
    };

    public static Activity tryToGetActivity(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return tryToGetActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    private final LifecycleObserver mLifecycleObserver = new LifecycleObserver() {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        public void lifecyclePause() {
            mAllowRendering = false;
            Activity activity = tryToGetActivity(mView.getContext());
            if (activity != null) {
                if (activity.isFinishing()) {
                    //Only clear when we're leaving, not when we leave the app or turn the display off.
                    try {
                        mHmdService.clearHudDisplay();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        public void lifecycleResume() {
            mAllowRendering = true;
            queueDraw();
        }
    };

    private final ViewTreeObserver.OnDrawListener onDrawListener = new ViewTreeObserver.OnDrawListener() {
        @Override
        public void onDraw() {
//            Log.i(TAG, "onDraw: called in listener");
            if (!mIsEnabled) {
                return;
            }
            //Post to the main thread so we queue the draw AFTER the draw not before. onDraw is called before for whatever reason.
            mMainHandler.post(mDrawRunnable);
        }
    };

    private final Runnable mDrawRunnable = new Runnable() {
        @Override
        public void run() {
            //Queue a draw on our worker thread.
            queueDraw();
        }
    };

    private void queueDraw() {
        if (!mIsEnabled) {
            return;
        }
        if (mUsePixelCopy && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mBackgroundHandler != null) {
                mBackgroundHandler.post(mDrawBackgroundRunnable);
            }
        } else {
            Rect size = updateDrawingBitmapIfNeeded();
            if (size != null) {
                mDrawingCanvas.drawColor(Color.BLACK);
                mView.draw(mDrawingCanvas);
                if (mAllowRendering && mIsEnabled) {
                    drawOntoHud();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private final Runnable mDrawBackgroundRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            if (mBackgroundHandler != null) {
                mBackgroundHandler.removeCallbacks(this);
                triggerPixelCopy();
            }
        }
    };


    private final HudCallbacks mCallbacks = new HudCallbacks(Looper.getMainLooper()) {
        @Override
        public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
            mHmdService = hmdService;
            if (mHmdService != null) {
                try {
                    mHmdService.setHudOperationMode(EnumHudMode.HUD_MODE_NORMAL.value(), false);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onConnected(boolean connected) throws RemoteException {
            super.onConnected(connected);
            if (connected) {
                queueDraw();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void triggerPixelCopy() {

        Activity activity = tryToGetActivity(mView.getContext());
        if (activity == null) {
            Log.i(TAG, "triggerPixelCopy: Can't find activity");
            return;
        }
        if (mHmdService == null) {
            return;
        }

        Rect rect = updateDrawingBitmapIfNeeded();
        if (rect == null) {
            return;
        }
        Log.i(TAG, "triggerPixelCopy: Starting pixel draw: w:" + rect.width() + " h:" + rect.height());
        if (mUsePixelCopy) {
            try {
                PixelCopy.request(activity.getWindow(), rect, mDrawingBitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                    @Override
                    public void onPixelCopyFinished(int copyResult) {
                        if (copyResult == PixelCopy.SUCCESS) {
                            if (mAllowRendering && mIsEnabled) {
//                                mBackgroundHandler.post(new Runnable() {
//                                    @Override
//                                    public void run() {
                                drawOntoHud();
//                                    }
//                                });
                            }
                        } else {
                            Log.w(TAG, "onPixelCopyFinished() called with error: copyResult = [" + copyResult + "]");
                        }
//                        Log.i(TAG, "onPixelCopyFinished: Pixel copy and draw complete");
                    }
                }, mBackgroundHandler);
            } catch (IllegalArgumentException e) {
                //We don't care about "Window doesn't have a backing surface!", we'll just ignore this draw.
                //As far as I can tell, there is no way to check for this beforehand.
                //This can also happen if the view hasn't drawn yet.
            }
        }
    }

    private Rect updateDrawingBitmapIfNeeded() {
        int[] loc_xy = new int[2];
        mView.getLocationInWindow(loc_xy);
        Rect rect = new Rect(loc_xy[0], loc_xy[1], loc_xy[0] + mView.getWidth(), loc_xy[1] + mView.getHeight());
        if (rect.height() == 0 || rect.width() == 0) {
            return null;
        }
        if (mDrawingBitmap == null || mDrawingBitmap.getWidth() != rect.width() || mDrawingBitmap.getHeight() != rect.height()) {
            if (mDrawingBitmap != null) {
                mDrawingBitmap.recycle();
                mDrawingBitmap = null;
            }
            mDrawingBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
            if (mUseCorrectionCanvas) {
                mDrawingCanvas = new HudWhiteTextCanvas(mDrawingBitmap);
            } else {
                mDrawingCanvas = new Canvas(mDrawingBitmap);

            }
        }
        return rect;
    }

    private void drawOntoHud() {
        Bitmap bitmap = HudBitmapHelper.calculateAdjustedBitmap(mDrawingBitmap, null, null, mDrawingPaint);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, mByteOutput);
        /* send image to app for update */
        mJpegFrame.set_byte(mByteOutput.toByteArray());
        mByteOutput.reset();
        if (mHmdService != null) {
            try {
//                Log.i(TAG, "drawOntoHud: Sending to Hud");
                mHmdService.sendBufferToHud(mJpegFrame);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void startMirroring() {
        Log.d(TAG, "startMirroring() called");
        mIsEnabled = true;
        queueDraw();
    }

    public void stopMirroring() {
        Log.d(TAG, "stopMirroring() called");
        mIsEnabled = false;
        mMainHandler.removeCallbacks(mDrawRunnable);
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBackgroundHandler.removeCallbacks(mDrawBackgroundRunnable);
            }
        }
    }
}

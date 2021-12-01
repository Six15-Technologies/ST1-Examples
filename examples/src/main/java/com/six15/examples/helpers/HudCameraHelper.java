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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudServiceConnection;
import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.CameraResolution;
import com.six15.hudservice.IHudService;

import java.util.List;

public class HudCameraHelper {
    private static final String TAG = HudCameraHelper.class.getSimpleName();
    private final HudServiceConnection mHudServiceConnectionCamera;
    private HandlerThread mBitmapHandlerThread;
    private Handler mBitmapHandler;
    private IHudService mHmdService;
    private boolean mDeviceConnected;
    private Bitmap mCameraBitmap;
    private final Handler mHandlerMainHandler;
    private final Callbacks mCallbacks;

    public static abstract class Callbacks {
        //If you return true, then you allow any previous bitmap to be recycled.
        //This greatly reduces peak memory usage since we can manually free bitmaps
        //without relying on the garbage collector.
        protected abstract boolean onCameraBitmap(@Nullable Bitmap bitmap);

        @Nullable
        protected CameraResolution getCameraResolution(@NonNull List<CameraResolution> resolutions) {
            if (resolutions.size() == 0) {
                return null;
            }
            return resolutions.get(0);
        }

        protected boolean isReadyForCamera() {
            return true;
        }
    }

    public HudCameraHelper(Context context, @NonNull Callbacks callbacks) {
        mCallbacks = callbacks;
        mHandlerMainHandler = new Handler(Looper.getMainLooper());
        mHudServiceConnectionCamera = new HudServiceConnection(context.getApplicationContext(), mCameraCallbacks);
    }

    private final HudCallbacks mCameraCallbacks = new HudCallbacks(Looper.getMainLooper()) {
        @Override
        public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
            mHmdService = hmdService;
        }

        @Override
        public void onConnected(boolean connected) throws RemoteException {
            super.onConnected(connected);
            mDeviceConnected = connected;
            if (connected) {
                startCameraIfReady();
            } else {
                updateBitmapFromThread(null);
            }
        }

        @Override
        public void onJpeg(ByteFrame byteFrame) throws RemoteException {
            super.onJpeg(byteFrame);
            if (mBitmapHandler == null) {
                return;
            }
            mBitmapHandler.removeCallbacksAndMessages(null);
            mBitmapHandler.post(new Runnable() {
                @Override
                public void run() {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(byteFrame.jpgBuffer, 0, byteFrame.jpgBuffer.length);
                    updateBitmapFromThread(bitmap);
                }
            });
        }
    };


    public void start() {
        mBitmapHandlerThread = new HandlerThread(HudCameraHelper.class.getName() + "_BitmapThread");
        mBitmapHandlerThread.start();
        mBitmapHandler = new Handler(mBitmapHandlerThread.getLooper());
        mHudServiceConnectionCamera.connectToService();
    }

    public boolean startCameraIfReady() {
        boolean started = false;
        if (mHmdService != null && mDeviceConnected && mCallbacks.isReadyForCamera()) {
            try {
                List<CameraResolution> resolutions = mHmdService.getSupportedCameraResolutions();
                if (resolutions != null) {
                    CameraResolution res = mCallbacks.getCameraResolution(resolutions);
                    if (res != null) {
                        mHmdService.setCameraResolution(res);
                        mHmdService.startRawCameraCapture();//Raw capture comes back in onJpeg
                        //mHmdService.startCameraCapture();//Normal captures comes back in onImage
                        started = true;
                    }
                }
            } catch (RemoteException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return started;
    }

    public void stopCamera() {
        if (mHmdService != null && mDeviceConnected) {
            try {
                mHmdService.stopCamera();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateBitmapFromThread(@Nullable Bitmap bitmap) {
        mHandlerMainHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean canRecycle = mCallbacks.onCameraBitmap(bitmap);
                if (canRecycle && mCameraBitmap != null && mCameraBitmap != bitmap) {
                    mCameraBitmap.recycle();
                }
                mCameraBitmap = bitmap;
            }
        });
    }

    public void stop() {
        mHudServiceConnectionCamera.disconnectFromService();
        mBitmapHandler = null;
        mBitmapHandlerThread.quit();
        mBitmapHandlerThread = null;
    }
}

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
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudServiceConnection;
import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.CameraResolution;
import com.six15.hudservice.IHudService;
import com.six15.hudservice.ImageFrame;

import java.util.List;

public class HudCameraHelper {
    private static final String TAG = HudCameraHelper.class.getSimpleName();
    private final HudServiceConnection mHudServiceConnectionCamera;
    private final boolean mUseBitmapFormat;
    private IHudService mHmdService;
    private boolean mDeviceConnected;
    private Bitmap mCameraBitmap;
    private final Handler mCallbackHandler;
    private final Callbacks mCallbacks;
    private CameraResolution mSelectedRes;

    public static abstract class Callbacks {
        //If you return true, then you allow the previous bitmap to be recycled.
        //This greatly reduces peak memory usage since we can manually free bitmaps
        //without relying on the garbage collector.
        protected boolean onCameraBitmap(@Nullable Bitmap bitmap) {
            return true;
        }

        protected void onCameraJpeg(@Nullable byte[] jpeg_byes) {
        }

        @Nullable
        protected CameraResolution getCameraResolution(@NonNull List<CameraResolution> resolutions) {
            if (resolutions.size() == 0) {
                return null;
            }
            return resolutions.get(0);
        }

        //By default, assume always ready for the camera. So whenever an ST1 connects, the camera should start.
        //If you ever return false, you can later call HudCameraHelper.startCameraIfReady() to
        //trigger another call to this function where you can return true.
        protected boolean isReadyForCamera() {
            return true;
        }

    }

    //The native format of the camera is JPEG. (it's an MJPEG Camera).
    //Bitmaps are RGB under the hood, so the service needs to decompress the JPEG before sending it.
    //The Bitmap class provides an optimized implementation of the Parcelable interface which reduces memory copying when using AIDL based interprocess communication.
    //This makes transferring Bitmaps very efficient, especially if the image is needed in the Bitmap format for an ImageView or Canvas.
    //Bitmaps do use more memory that JPEGs since they are larger. To help avoid excessive memory allocation, HudCameraHelper's onCameraBitmap() callback can return a boolean.
    //If true is returned, HudCameraHelper recycles the previous (not the one just delivered) bitmap. This dramatically reduces peak memory usage when streaming video.
    public HudCameraHelper(@NonNull Context context, boolean useBitmapFormat, @NonNull Callbacks callbacks) {
        this(context, useBitmapFormat, callbacks, (Looper) null);
    }

    public HudCameraHelper(@NonNull Context context, boolean useBitmapFormat, @NonNull Callbacks callbacks, @Nullable Looper callbackLooper) {
        this(context, useBitmapFormat, callbacks, callbackLooper == null ? null : new Handler(callbackLooper));
    }

    public HudCameraHelper(@NonNull Context context, boolean useBitmapFormat, @NonNull Callbacks callbacks, @Nullable final Handler callbackHandler) {
        mCallbacks = callbacks;
        if (callbackHandler == null) {
            mCallbackHandler = new Handler(Looper.getMainLooper());
        } else {
            mCallbackHandler = callbackHandler;
        }
        mUseBitmapFormat = useBitmapFormat;
        mHudServiceConnectionCamera = new HudServiceConnection(context.getApplicationContext(), getHudCallbacks());
    }

    private HudCallbacks getHudCallbacks() {
        return new HudCallbacks(mCallbackHandler) {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
            }

            @Override
            public void onConnected(boolean connected) throws RemoteException {
                super.onConnected(connected);
                mDeviceConnected = connected;
                if (connected) {
                    if (mHmdService != null) {
                        //Higher 720p video frame rate may be disabled on some devices since old Zebra SDK's / Hardware didn't properly support them.
                        //Always reset the value to the desired 24 fps. This value is persisted in flash on the ST1 (or HD4000) device.
                        mHmdService.set720Framerate((byte) 24);
                    }
                    startCameraIfReady();
                } else {
                    if (mUseBitmapFormat) {
                        mCallbacks.onCameraBitmap(null);
                    } else {
                        mCallbacks.onCameraJpeg(null);
                    }
                }
            }

            @Override
            public void onJpeg(ByteFrame byteFrame) throws RemoteException {
                super.onJpeg(byteFrame);
                if (mUseBitmapFormat) {
                    //This can happen if multiple HudCameraHelpers are used in the same process, but use different formats.
                    return;
                }
                if (byteFrame == null) {
                    return;
                }
                byte[] bytes = byteFrame.get_byte();
                if (bytes == null) {
                    return;
                }
                mCallbacks.onCameraJpeg(bytes);
            }

            @Override
            public void onImage(ImageFrame imageFrame) throws RemoteException {
                super.onImage(imageFrame);
                if (!mUseBitmapFormat) {
                    //This can happen if multiple HudCameraHelpers are used in the same process, but use different formats.
                    return;
                }
                if (imageFrame == null) {
                    return;
                }
                Bitmap bitmap = imageFrame.imageBitmap;
                if (bitmap == null) {
                    return;
                }
                callBitmapCallback(imageFrame.imageBitmap);
            }
        };
    }

    public void connect() {
        mHudServiceConnectionCamera.connectToService();
    }

    public boolean startCameraIfReady() {
        boolean started = false;
        if (mHmdService != null && mDeviceConnected && mCallbacks.isReadyForCamera()) {
            try {
                List<CameraResolution> resolutions = mHmdService.getSupportedCameraResolutions();
                if (resolutions != null) {
                    mSelectedRes = mCallbacks.getCameraResolution(resolutions);
                    if (mSelectedRes != null) {
                        mHmdService.setCameraResolution(mSelectedRes);
                        if (mUseBitmapFormat) {
                            mHmdService.startCameraCapture();//Normal captures comes back in onImage
                        } else {
                            mHmdService.startRawCameraCapture();//Raw capture comes back in onJpeg
                        }
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

    private void callBitmapCallback(@NonNull Bitmap bitmap) {
        boolean canRecycle = mCallbacks.onCameraBitmap(bitmap);
        if (canRecycle && mCameraBitmap != null && mCameraBitmap != bitmap) {
            mCameraBitmap.recycle();
        }
        mCameraBitmap = bitmap;
    }

    public void disconnect() {
        mHudServiceConnectionCamera.disconnectFromService();
    }
}

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

package com.six15.examples;

import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudService;
import com.six15.examples.connection.HudServiceConnection;
import com.six15.examples.helpers.HudDisplayHelper;
import com.six15.hudservice.Constants;
import com.six15.hudservice.EnumHudMode;
import com.six15.hudservice.IHudService;

//This class extends HudService, but doesn't use or override any of its functionality.
//Feel free to change HudService to another class.
public abstract class HudPresentingService extends HudService implements DisplayManager.DisplayListener {
    private IHudService mHmdService;
    private DisplayManager mDisplayManager;
    private boolean mIsPresenting = false;
    private int mDisplayId;
    private Presentation mPresentation;
    private Handler mHandler;
    private static final String TAG = HudPresentingService.class.getSimpleName();
    private HudServiceConnection mConnection;
    private boolean mConnected;

    @Override
    public void onCreate() {
        mConnection = new HudServiceConnection(this, new HudCallbacks() {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
            }

            @Override
            public void onConnected(boolean connected) throws RemoteException {
                super.onConnected(connected);
                mConnected = connected;
                EnumHudMode operationalMode = connected ? EnumHudMode.HUD_MODE_PRESENT : EnumHudMode.HUD_MODE_NORMAL;
                try {
                    if (mHmdService != null) {
                        mHmdService.setHudOperationMode(operationalMode.value(), false);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onHudOperationModeChanged(byte mode) throws RemoteException {
                if (mConnected && mode != EnumHudMode.HUD_MODE_PRESENT.value() && mode != EnumHudMode.HUD_MODE_MIRROR.value()) {
                    //It seems like someone else is trying to use the HUD. So just exit.
                    stopSelf();
                }
            }
        });
        mConnection.connectToService();

        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        mDisplayManager.registerDisplayListener(this, mHandler);

        Display hudDisplay = HudDisplayHelper.getHudPresentationDisplay(mDisplayManager);
        if (hudDisplay != null) {
            startPresenting(hudDisplay);
        } else {
            try {
                if (mHmdService != null) {
                    mHmdService.setHudOperationMode(EnumHudMode.HUD_MODE_PRESENT.value(), false);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisplayManager.unregisterDisplayListener(this);
        if (mPresentation != null) {
            mPresentation.dismiss();
            mIsPresenting = false;
            mPresentation = null;
            try {
                mHmdService.setHudOperationMode(EnumHudMode.HUD_MODE_NORMAL.value(), false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mConnection.disconnectFromService();
    }

    private boolean isHudDisplay(@NonNull Display display) {
        return Constants.VIRTUAL_DISPLAY_NAME.equals(display.getName());
    }

    @Override
    public void onDisplayAdded(int displayId) {
        Display display = mDisplayManager.getDisplay(displayId);
        if (isHudDisplay(display)) {
            if (!mIsPresenting) {
                startPresenting(display);
            }
        }
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        if (mIsPresenting && mDisplayId == displayId) {
            mIsPresenting = false;
        }
    }

    @Override
    public void onDisplayChanged(int displayId) {

    }

    private void startPresenting(Display hudDisplay) {
        if (mIsPresenting) {
            return;
        }
        mDisplayId = hudDisplay.getDisplayId();
        mIsPresenting = true;
        mPresentation = getPresentation(this, hudDisplay);
        mPresentation.show();
    }

    public abstract Presentation getPresentation(Context outerContext, Display hudDisplay);
}
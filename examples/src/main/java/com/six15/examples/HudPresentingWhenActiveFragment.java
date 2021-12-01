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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudRetainedFragment;
import com.six15.hudservice.EnumHudMode;
import com.six15.hudservice.IHudService;
import com.six15.examples.helpers.HudDisplayHelper;

//This fragment always starts a Presentation on any HUD it sees.
//The presentation runs as long as this fragment is "active" i.e. not paused for longer than needed for a configuration change of the parent activity.
public abstract class HudPresentingWhenActiveFragment extends HudRetainedFragment implements DisplayManager.DisplayListener {

    private DisplayManager mDisplayManager;
    private IHudService mHmdService;
    private boolean mIsPresenting = false;
    private Presentation mPresentation;
    private Handler mHandler;
    private final Runnable mStopPresentingRunnable = this::stopPresenting;
    private int mDisplayId;

    @Override
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mDisplayManager = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        super.onCreate(savedInstanceState);
    }

    @Override
    @CallSuper
    public void onResume() {
        super.onResume();
        mHandler.removeCallbacks(mStopPresentingRunnable);
        mDisplayManager.registerDisplayListener(this, mHandler);
        if (mIsPresenting) {
            return;
        }

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
    }

    @Override
    @CallSuper
    public void onPause() {
        mDisplayManager.unregisterDisplayListener(this);
        mHandler.postDelayed(mStopPresentingRunnable, 250);
        super.onPause();
    }

    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mStopPresentingRunnable);
        stopPresenting();
        mDisplayManager = null;
    }

    private void stopPresenting() {
        if (mPresentation != null) {
            mPresentation.dismiss();
            mIsPresenting = false;
            mPresentation = null;
            try {
                if (mHmdService != null) {
                    mHmdService.setHudOperationMode(EnumHudMode.HUD_MODE_NORMAL.value(), false);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisplayAdded(int displayId) {
        Display display = mDisplayManager.getDisplay(displayId);
        if (display == null) {
            return;
        }
        if (HudDisplayHelper.isHudDisplay(display)) {
            if (mIsPresenting) {
                stopPresenting();
            }
            startPresenting(display);
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

    @NonNull
    @Override
    final protected HudCallbacks getCallbacks() {
        return new HudCallbacks() {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
            }

            @Override
            public void onConnected(boolean connected) throws RemoteException {
                super.onConnected(connected);
                EnumHudMode operationalMode = connected ? EnumHudMode.HUD_MODE_PRESENT : EnumHudMode.HUD_MODE_NORMAL;
                try {
                    if (mHmdService != null) {
                        mHmdService.setHudOperationMode(operationalMode.value(), false);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void startPresenting(Display hudDisplay) {
        mHandler.removeCallbacks(mStopPresentingRunnable);
        if (mIsPresenting) {
            return;
        }
        mDisplayId = hudDisplay.getDisplayId();
        mIsPresenting = true;
        mPresentation = getPresentation(getActivity(), hudDisplay);

        try {
            mPresentation.show();
        } catch (WindowManager.InvalidDisplayException e) {
            e.printStackTrace();

            //In some situations this can happen if modes are changing too fast.
            //Stop presenting, and re-start presentation mode.
            stopPresenting();
            try {
                if (mHmdService != null) {
                    mHmdService.setHudOperationMode(EnumHudMode.HUD_MODE_PRESENT.value(), false);
                }
            } catch (RemoteException e2) {
                e2.printStackTrace();
            }
        }
    }

    public abstract Presentation getPresentation(Context outerContext, Display hudDisplay);

}

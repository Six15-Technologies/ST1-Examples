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

package com.six15.examples_test.screen_mirroring;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudCompatActivity;
import com.six15.examples_test.R;
import com.six15.hudservice.EnumHudMode;
import com.six15.hudservice.IHudService;

public class ScreenMirroringActivity extends HudCompatActivity {

    private static final String TAG = ScreenMirroringActivity.class.getSimpleName();
    private IHudService mHmdService;
    private CompoundButton mButton;
    private EnumHudMode mOpMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_button);
        mButton = findViewById(R.id.activity_button_toggle_button);
        mButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EnumHudMode mode = isChecked ? EnumHudMode.HUD_MODE_MIRROR_ONLY : EnumHudMode.HUD_MODE_NORMAL;
                if (mode == mOpMode) {
                    return;
                }
                if (mHmdService != null) {
                    try {
                        mHmdService.setHudOperationMode(mode.value(), false);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    protected @NonNull HudCallbacks getCallbacks() {
        return new HudCallbacks(Looper.getMainLooper()) {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
                if (mHmdService != null) {
                    try {
                        EnumHudMode mode = EnumHudMode.forValue((byte) mHmdService.getHudOperationMode());
                        if (mode != null) {
                            mOpMode = mode;
                        } else {
                            mOpMode = EnumHudMode.HUD_MODE_NORMAL;
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                updateUi();
            }

            @Override
            public void onHudOperationModeChanged(byte mode) throws RemoteException {
                EnumHudMode operationalMode = EnumHudMode.forValue(mode);
                if (operationalMode == null) {
                    Log.e(TAG, "Unknown operationalMode " + mode);
                    return;
                }
                mOpMode = operationalMode;
                updateUi();
            }
        };
    }

    private void updateUi() {
        if (mButton != null) {
            mButton.setChecked(mOpMode == EnumHudMode.HUD_MODE_MIRROR_ONLY);
            mButton.setEnabled(mHmdService != null);
        }
    }
}

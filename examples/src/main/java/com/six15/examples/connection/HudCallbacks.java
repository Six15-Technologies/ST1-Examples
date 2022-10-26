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

package com.six15.examples.connection;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.Cam720FPS;
import com.six15.hudservice.DebugMessage;
import com.six15.hudservice.DeviceExtraInfo;
import com.six15.hudservice.DeviceInfo;
import com.six15.hudservice.DeviceSettings;
import com.six15.hudservice.DisplayAggressiveSleepState;
import com.six15.hudservice.DisplayBrightness;
import com.six15.hudservice.HUD_CommandBYTE;
import com.six15.hudservice.IHudService;
import com.six15.hudservice.IHudServiceCallback;
import com.six15.hudservice.ImageFrame;
import com.six15.hudservice.ImuCal;
import com.six15.hudservice.ImuData;
import com.six15.hudservice.ImuStatus;

public abstract class HudCallbacks extends IHudServiceCallback.Stub {
    private static final String TAG = HudCallbacks.class.getSimpleName();
    private static final boolean DEBUG = false;
    private HudCallbacks mProxyCallbacks;

    public HudCallbacks(@NonNull Context context) {
        this(context.getMainLooper());
    }

    public HudCallbacks() {
        this((Looper) null);
    }

    HudCallbacks getProxyCallbacks() {
        if (mProxyCallbacks != null) {
            return mProxyCallbacks;
        } else {
            return this;
        }
    }

    public HudCallbacks(@Nullable Looper looper) {
        this(looper == null ? null : new Handler(looper));
    }

    public HudCallbacks(@Nullable final Handler handler) {
        if (handler != null) {
            final HudCallbacks innerProxyCallbacks = this;
            mProxyCallbacks = new HudCallbacks() {

                @Override
                public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                    handler.post(() -> {
                        innerProxyCallbacks.onServiceConnectionChanged(available, hmdService, launchIntentForPermissions);
                    });
                }

                @Override
                public void onCamera(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onCamera(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onAudio(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onAudio(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onPing(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onPing(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onInfo(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onInfo(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onDisplay(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onDisplay(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onSettings(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onSettings(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onImuData(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onImuData(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onData(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onData(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onImage(ImageFrame imageFrame) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onImage(imageFrame);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onJpeg(ByteFrame byteFrame) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onJpeg(byteFrame);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onDebugTerminal(byte cmd, String jsonText) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onDebugTerminal(cmd, jsonText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onConnected(boolean connected) throws RemoteException {
                    super.onConnected(connected);
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onConnected(connected);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onHudOperationModeChanged(byte mode) throws RemoteException {
                    handler.post(() -> {
                        try {
                            innerProxyCallbacks.onHudOperationModeChanged(mode);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }
            };
        }
    }

    //This callback isn't part of IHudServiceCallback.
    public abstract void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions);

    @Override
    public void onCamera(byte cmd, String test) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onCamera() called with: cmd = [" + cmd + "], test = [" + test + "]");
        onJsonCommand(cmd, test);
    }

    @Override
    public void onAudio(byte cmd, String test) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onAudio() called with: cmd = [" + cmd + "], test = [" + test + "]");
        onJsonCommand(cmd, test);
    }

    @Override
    public void onPing(byte cmd, String test) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onPing() called with: cmd = [" + cmd + "], test = [" + test + "]");
    }

    @Override
    public void onInfo(byte cmd, String test) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onInfo() called with: cmd = [" + cmd + "], test = [" + test + "]");
        onJsonCommand(cmd, test);
    }

    @Override
    public void onDisplay(byte cmd, String test) throws RemoteException {
        if (DEBUG)
            Log.d(TAG, "onDisplay() called with: cmd = [" + cmd + "], test = [" + test + "]");
        onJsonCommand(cmd, test);
    }

    @Override
    public void onSettings(byte cmd, String test) throws RemoteException {
        if (DEBUG)
            Log.d(TAG, "onSettings() called with: cmd = [" + cmd + "], test = [" + test + "]");
        onJsonCommand(cmd, test);

    }

    @Override
    public void onImuData(byte cmd, String test) throws RemoteException {
        if (DEBUG)
            Log.d(TAG, "onImuData() called with: cmd = [" + cmd + "], test = [" + test + "]");
        onJsonCommand(cmd, test);
    }

    @Override
    public void onData(byte cmd, String test) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onData() called with: cmd = [" + cmd + "], test = [" + test + "]");
        onJsonCommand(cmd, test);
    }

    @Override
    public void onImage(ImageFrame imageFrame) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onImage() called with: imageFrame = [" + imageFrame + "]");
    }

    @Override
    public void onJpeg(ByteFrame byteFrame) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onJpeg() called with: byteFrame = [" + byteFrame + "]");
    }

    @Override
    public void onDebugTerminal(byte cmd, String test) throws RemoteException {
        if (DEBUG)
            Log.d(TAG, "onDebugTerminal() called with: cmd = [" + cmd + "], test = [" + test + "]");
        onJsonCommand(cmd, test);
    }

    @Override
    public void onConnected(boolean connected) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onConnected() called with: connected = [" + connected + "]");
    }

    @Override
    public void onHudOperationModeChanged(byte mode) throws RemoteException {
        if (DEBUG) Log.d(TAG, "onHudOperationModeChanged() called with: mode = [" + mode + "]");
    }

    private void onJsonCommand(byte cmd, String jsonText) throws RemoteException {
        HUD_CommandBYTE command = HUD_CommandBYTE.forValue(cmd);
        if (command == null) {
            //Unknown command
            return;
        }
        try {

            switch (command) {
                case HC_DEV_INFO:
                    DeviceInfo deviceInfo = new Gson().fromJson(jsonText, DeviceInfo.class);
                    onDeviceInfo(deviceInfo);
                    break;
                case HC_DEV_SETTINGS:
                    DeviceSettings deviceSettings = new Gson().fromJson(jsonText, DeviceSettings.class);
                    onDeviceSettings(deviceSettings);
                    break;
                case HC_VERSIONS_EXTRA:
                    DeviceExtraInfo deviceExtraInfo = new Gson().fromJson(jsonText, DeviceExtraInfo.class);
                    onDeviceExtraInfo(deviceExtraInfo);
                    break;
                case HC_IMU_DATA:
                    ImuData imuData = new Gson().fromJson(jsonText, ImuData.class);
                    onImuData(imuData);
                    break;
                case HC_DEV_DEBUG_TERMINAL:
                    DebugMessage debugMessage = new Gson().fromJson(jsonText, DebugMessage.class);
                    onDebugMessage(debugMessage);
                    break;
                case HC_IMU_STATUS:
                    ImuStatus imuStatus = new Gson().fromJson(jsonText, ImuStatus.class);
                    onImuStatus(imuStatus);
                    break;
                case HC_IMU_GET_CAL:
                    ImuCal imuCal = new Gson().fromJson(jsonText, ImuCal.class);
                    onImuCal(imuCal);
                    break;
                case HC_DISP_AGGRESSIVE_SLEEP:
                    DisplayAggressiveSleepState displayAggressiveSleepState = new Gson().fromJson(jsonText, DisplayAggressiveSleepState.class);
                    onDisplay(displayAggressiveSleepState);
                    break;
                case HC_DISP_BRT:
                    DisplayBrightness displayBrightness = new Gson().fromJson(jsonText, DisplayBrightness.class);
                    onDisplay(displayBrightness);
                    break;
                case HC_DEV_720_FPS:
                    Cam720FPS cam720FPS = new Gson().fromJson(jsonText, Cam720FPS.class);
                    onCam720pFPS(cam720FPS);
                    break;
                default:
                    if (DEBUG)
                        Log.i(TAG, String.format("Unhandled JSON:%x %d : %s", cmd, cmd, jsonText));
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "onJsonCommand: NumberFormatException cmd:" + command + " str:" + jsonText);
        } catch (JsonSyntaxException e) {
            Log.w(TAG, "onJsonCommand: JsonSyntaxException cmd:" + command + " str:" + jsonText);
        }
    }

    public void onDisplay(@Nullable DisplayBrightness displayBrightness) throws RemoteException {
    }

    public void onDisplay(@Nullable DisplayAggressiveSleepState displayAggressiveSleepState) throws RemoteException {
    }

    public void onImuStatus(@Nullable ImuStatus imuStatus) throws RemoteException {
    }

    public void onImuCal(@Nullable ImuCal imuCal) throws RemoteException {
    }

    public void onDebugMessage(@Nullable DebugMessage debugMessage) throws RemoteException {
    }

    public void onImuData(@Nullable ImuData imuData) throws RemoteException {
    }

    public void onDeviceExtraInfo(@Nullable DeviceExtraInfo deviceExtraInfo) throws RemoteException {
    }

    public void onDeviceSettings(@Nullable DeviceSettings deviceSettings) throws RemoteException {
    }

    public void onDeviceInfo(@Nullable DeviceInfo deviceInfo) throws RemoteException {
    }

    public void onCam720pFPS(@Nullable Cam720FPS cam720FPS) throws RemoteException {
    }
}

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

package com.six15.vosk_speech_recognition;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.gson.JsonParser;
import com.six15.hudservice.Constants;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.Assets;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

public class HudSpeechRecognitionHelper implements RecognitionListener {

    private static final String TAG = HudSpeechRecognitionHelper.class.getSimpleName();
    private final Thread mPrepareThread;
    private SpeechService mSpeechService;
    private final Handler mMainHandler;
    private boolean mRecording;
    private final Context mContext;
    public AudioDeviceInfo mCurrentAudioDevice = null;

    private final boolean PREFER_SIX15_MIC_OVER_OTHER_EXTERNAL = false;
    private final Callback mCallback;
    private String mLastPartial = "";

    public HudSpeechRecognitionHelper(@NonNull Context context, @Nullable Callback callback, @Nullable Collection<String> commandGrammar) {
        mCallback = callback;
        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());

        mPrepareThread = new Thread(() -> {
            prepareSpeechService(mContext, commandGrammar);
            mMainHandler.post(() -> {
                if (mRecording) {
                    changeRecognitionDevice(getOptimalExternalMic(mContext, PREFER_SIX15_MIC_OVER_OTHER_EXTERNAL));
                }
            });
        });
        mPrepareThread.start();

        mRecording = false;

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.registerAudioDeviceCallback(new AudioDeviceCallback() {
                @Override
                public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                    super.onAudioDevicesAdded(addedDevices);
                    if (mRecording) {
                        AudioDeviceInfo newDevice = getOptimalExternalMic(mContext, PREFER_SIX15_MIC_OVER_OTHER_EXTERNAL);
                        if (mCurrentAudioDevice != newDevice) {
                            changeRecognitionDevice(newDevice);
                        }
                    }
                }

                @Override
                public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                    super.onAudioDevicesRemoved(removedDevices);
                    if (mRecording) {
                        AudioDeviceInfo newDevice = getOptimalExternalMic(mContext, PREFER_SIX15_MIC_OVER_OTHER_EXTERNAL);
                        if (mCurrentAudioDevice != newDevice) {
                            changeRecognitionDevice(newDevice);
                        }
                    }
                }
            }, mMainHandler);
        }
    }

    private void prepareSpeechService(Context context, Collection<String> commandGrammar) {
        Assets assets;
        try {
            assets = new Assets(context);
            File assetDir = assets.syncAssets();
            Model voiceModel = new Model(assetDir.toString() + "/model-android");

            Recognizer voiceRecognizer;
            if (commandGrammar != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");

                sb.append("\"");
                sb.append("[unk]");
                sb.append("\"");
                sb.append(", ");

                for (Iterator<String> iterator = commandGrammar.iterator(); iterator.hasNext(); ) {
                    String phrase = iterator.next();
                    sb.append("\"");
                    sb.append(phrase.toLowerCase());
                    sb.append("\"");
                    if (iterator.hasNext()) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
                voiceRecognizer = new Recognizer(voiceModel, 16000.0f, sb.toString());
            } else {
                voiceRecognizer = new Recognizer(voiceModel, 16000.0f);
            }

            LibVosk.setLogLevel(LogLevel.INFO);
            mSpeechService = new SpeechService(voiceRecognizer, 16000.0f);
            mSpeechService.addListener(this);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Vosk model failed to link. Device may be too old for libvosk to run");
        }
    }


    private void changeRecognitionDevice(AudioDeviceInfo audioDevice) {
        try {
            mPrepareThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mSpeechService == null) {
            Log.e(TAG, "changeRecognitionDevice: mSpeechService must not be null");
            mCallback.onError();
            return;
        }
        mSpeechService.cancel();
        mRecording = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDevice != null) {
//            Toast.makeText(mContext, "Using mic:" + audioDevice.getProductName(), Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Using mic:" + audioDevice.getProductName());
            setPreferredDeviceWithReflection(mSpeechService, audioDevice);
        }
        if (mCallback != null) {
            mCallback.onMicChanged(audioDevice);
        }
        mCurrentAudioDevice = audioDevice;
        mSpeechService.startListening();
        mRecording = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void setPreferredDeviceWithReflection(SpeechService service, AudioDeviceInfo audioDevice) {
        if (audioDevice == null) {
            Log.i(TAG, "No external mic requested");
            return;
        }
        try {
            Field recorderField = SpeechService.class.getDeclaredField("recorder");
            recorderField.setAccessible(true);
            AudioRecord recorder = (AudioRecord) recorderField.get(service);
            if (recorder == null) {
                Log.w(TAG, "Getting recorder with reflection failed");
                return;
            }
            boolean worked = recorder.setPreferredDevice(audioDevice);
            if (!worked) {
                Log.w(TAG, "Unable to request that the mic be used");
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private static AudioDeviceInfo getOptimalExternalMic(Context context, boolean preferSix15) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
            AudioDeviceInfo device = getOptimalExternalMic(devices, preferSix15);
//            Log.i(TAG, "getOptimalExternalMic found: " + (device == null ? null : device.getProductName()));
            return device;
        } else {
            //Android OS too old to getDevices
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static AudioDeviceInfo getOptimalExternalMic(AudioDeviceInfo[] devices, boolean preferSix15) {
        AudioDeviceInfo six15Mic = null;
        for (AudioDeviceInfo device : devices) {
            if (isSix15Mic(device)) {
                six15Mic = device;
                break;
            }
        }
        if (preferSix15 && six15Mic != null) {
            return six15Mic;
        }
        for (AudioDeviceInfo device : devices) {
//            Log.i(TAG, "getOptimalExternalMic: " + device.getProductName() + ":" + device.getType());
            if (isExternalMic(device) && !isSix15Mic(device)) {
                return device;
            }
        }
        //No other mic found, use the Six15 mic if its there, or just return null.
        return six15Mic;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static boolean isSix15Mic(@NonNull AudioDeviceInfo device) {
        return Constants.MIC_PRODUCT_NAME.contentEquals(device.getProductName());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static boolean isExternalMic(AudioDeviceInfo device) {
        switch (device.getType()) {
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
            case AudioDeviceInfo.TYPE_USB_DEVICE:
            case AudioDeviceInfo.TYPE_USB_HEADSET:
            case AudioDeviceInfo.TYPE_AUX_LINE:
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
            case AudioDeviceInfo.TYPE_HDMI:
                return true;
            default:
                return false;
        }
    }

    public void startRecognizing() {
        if (mSpeechService != null) {
            changeRecognitionDevice(getOptimalExternalMic(mContext, PREFER_SIX15_MIC_OVER_OTHER_EXTERNAL));
        }
        mRecording = true;
    }

    public void stopRecognizing() {
        mRecording = false;
        if (mSpeechService != null) {
            mSpeechService.cancel();
        }
    }

    public boolean isRecognizing() {
        return mRecording;
    }

    public void close() {
        try {
            mPrepareThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mSpeechService != null) {
            if (mRecording) {
                mSpeechService.cancel();
            }
            mSpeechService.shutdown();
        }
        mSpeechService = null;
    }

    @Override
    public void onPartialResult(String s) {
        String partial = JsonParser.parseString(s).getAsJsonObject().get("partial").getAsString();
        if (partial.equals("")) {
            return;
        }

        String nextPhrase;
        if (partial.startsWith(mLastPartial)) {
            nextPhrase = partial.substring(mLastPartial.length()).trim();
        } else {
            nextPhrase = partial;
        }
        if (nextPhrase.equals("")) {
            return;
        }

        mLastPartial = partial;
        if (mCallback != null) {
            mCallback.onVoiceCommand(nextPhrase);
        }
    }

    @Override
    public void onResult(String s) {
        mLastPartial = "";
    }

    @Override
    public void onError(Exception e) {
        Log.w(TAG, "onError() called with: e = [" + e + "]");
    }

    @Override
    public void onTimeout() {
        Log.w(TAG, "onTimeout() called");
    }

    public interface Callback {
        void onVoiceCommand(String phrase);

        void onMicChanged(@Nullable AudioDeviceInfo audioDevice);

        void onError();
    }
}

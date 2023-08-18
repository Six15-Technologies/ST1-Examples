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

package com.six15.examples_test.voice_to_text;

import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.six15.examples_test.R;
import com.six15.vosk_speech_recognition.HudSpeechRecognitionHelper;

import java.util.List;

public class VoiceToTextFragment extends Fragment {

    private String mLastVoiceCommand = null;
    private TextView mPhraseTextView;
    private HudSpeechRecognitionHelper mSpeechHelper;
    private TextView mMicLabel;
    private String mMicLabelString;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mSpeechHelper = new HudSpeechRecognitionHelper(requireContext(),
                new HudSpeechRecognitionHelper.Callback() {
                    @Override
                    public void onVoiceCommand(String phrase) {
                        mLastVoiceCommand = phrase;
                        updateUi();
                    }

                    @Override
                    public void onMicChanged(@Nullable AudioDeviceInfo audioDevice) {
                        if (audioDevice != null) {
                            mMicLabelString = (String) audioDevice.getProductName();
                        } else {
                            mMicLabelString = "null";
                        }
                        updateUi();
                    }

                    @Override
                    public void onError() {
                        Toast.makeText(VoiceToTextFragment.this.getContext(), "Failed to start Speech Recognition", Toast.LENGTH_SHORT).show();
                    }

                }, null);
    }

    private void updateUi() {
        if (mPhraseTextView != null) {
            mPhraseTextView.setText(mLastVoiceCommand);
        }
        if (mMicLabel != null) {
            mMicLabel.setText("Using Mic:" + mMicLabelString);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_voice_to_text, container, false);
        mPhraseTextView = rootView.findViewById(R.id.fragment_voice_to_text_phrase);
        mMicLabel = rootView.findViewById(R.id.fragment_voice_to_text_mic_label);
        updateUi();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mSpeechHelper.isRecognizing()) {
            mSpeechHelper.startRecognizing();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        boolean isFinishing = requireActivity().isFinishing();
        if (mSpeechHelper.isRecognizing() && isFinishing) {
            mSpeechHelper.stopRecognizing();
        }
    }

    @Override
    public void onDestroyView() {
        mPhraseTextView = null;
        mMicLabel = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSpeechHelper.isRecognizing()) {
            mSpeechHelper.stopRecognizing();
        }
    }
}

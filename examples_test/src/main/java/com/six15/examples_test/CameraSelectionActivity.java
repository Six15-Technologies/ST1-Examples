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

package com.six15.examples_test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.six15.examples_test.camera.CameraActivity;

public class CameraSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_selection);
    }

    private void startCameraActivity(int format) {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(CameraActivity.ARG_USE_WHICH_FRAGMENT, format);
        startActivity(intent);
    }


    public void startCameraBitmap(View view) {
        startCameraActivity(CameraActivity.WHICH_FRAGMENT_BITMAP);
    }

    public void startCameraJpeg(View view) {
        startCameraActivity(CameraActivity.WHICH_FRAGMENT_JPEG);
    }

    public void startCameraSurfaceView(View view) {
        startCameraActivity(CameraActivity.WHICH_FRAGMENT_SURFACE_VIEW);
    }

    public void startCameraTextureView(View view) {
        startCameraActivity(CameraActivity.WHICH_FRAGMENT_TEXTURE_VIEW);
    }

    public void startCameraSnapshot(View view) {
        startCameraActivity(CameraActivity.WHICH_FRAGMENT_SNAPSHOT);
    }
    public void startCameraScandit(View view) {
        startCameraActivity(CameraActivity.WHICH_FRAGMENT_SCANDIT);
    }
}
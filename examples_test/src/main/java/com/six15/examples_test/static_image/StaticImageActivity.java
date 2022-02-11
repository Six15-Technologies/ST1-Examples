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

package com.six15.examples_test.static_image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudCompatActivity;
import com.six15.examples.helpers.HudBitmapHelper;
import com.six15.examples_test.R;
import com.six15.hudservice.ByteFrame;
import com.six15.hudservice.EnumHudMode;
import com.six15.hudservice.IHudService;
import com.six15.hudservice.ImageFrame;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class StaticImageActivity extends HudCompatActivity {

    private IHudService mHmdService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);
    }

    @Override
    protected @NonNull HudCallbacks getCallbacks() {
        return new HudCallbacks(Looper.getMainLooper()) {
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
                if (connected) {
                    showImage();
                }
            }

            @Override
            public void onHudOperationModeChanged(byte mode) throws RemoteException {
                if (mode == EnumHudMode.HUD_MODE_NORMAL.value()) {
                    showImage();
                }
            }
        };
    }

    private void showImage() {
        if (isFinishing()) {
            return;
        }
        InputStream is = getResources().openRawResource(R.raw.test_image);
        Bitmap bitmap = BitmapFactory.decodeStream(is);
//        showImageWithAutoResize(bitmap);
        showImageWithManualCrop(bitmap);
    }

    private void showImageWithAutoResize(Bitmap bitmap) {
        try {
            mHmdService.setAutoResizeImage(true);
            mHmdService.sendImageToHud(new ImageFrame(bitmap));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void showImageWithManualCrop(Bitmap bitmap) {
        Bitmap resizedBitmap = HudBitmapHelper.calculateAdjustedBitmap(bitmap, null, null, new Paint());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos);
        byte[] jpgBuffer = baos.toByteArray();

        try {
            mHmdService.sendBufferToHud(new ByteFrame(jpgBuffer));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
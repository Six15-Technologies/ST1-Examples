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

package com.six15.examples_test.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudCompatActivity;
import com.six15.examples_test.R;
import com.six15.hudservice.IHudService;

public class CameraActivity extends HudCompatActivity {
    public static final String ARG_USE_WHICH_FRAGMENT = "ARG_USE_WHICH_FRAGMENT";
    public static final int WHICH_FRAGMENT_BITMAP = 0;
    public static final int WHICH_FRAGMENT_JPEG = 1;
    public static final int WHICH_FRAGMENT_SURFACE_VIEW = 2;
    public static final int WHICH_FRAGMENT_TEXTURE_VIEW = 3;
    public static final int WHICH_FRAGMENT_SNAPSHOT = 4;

    private static final int REQUEST_CODE_ALL_PERMISSION = 2;
    private boolean mNeedsToCreateFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        //Only first time. On secondary times the fragment manager handles it for us.
        mNeedsToCreateFragment = (savedInstanceState == null);

        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_ALL_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ALL_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mNeedsToCreateFragment) {
                    FragmentManager fm = getSupportFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();

                    //Using a fragment does 3 things for us here.
                    //1: Makes an easy to way to delay until permissions are accepted
                    //2: Allows permission code to be shared between camera examples.
                    //3: Makes it easy to persist across configuration changes with setRetainInstance(true)
                    Intent intent = getIntent();
                    if (intent == null) {
                        return;
                    }
                    int whichFragment = intent.getIntExtra(ARG_USE_WHICH_FRAGMENT, WHICH_FRAGMENT_BITMAP);
                    Class<? extends Fragment> fragmentClass;
                    switch (whichFragment) {
                        case WHICH_FRAGMENT_BITMAP:
                            fragmentClass = CameraBitmapFragment.class;
                            break;
                        case WHICH_FRAGMENT_JPEG:
                            fragmentClass = CameraJpegFragment.class;
                            break;
                        case WHICH_FRAGMENT_SNAPSHOT:
                            fragmentClass = CameraWithSnapshotFragment.class;
                            break;
                        case WHICH_FRAGMENT_SURFACE_VIEW:
                            fragmentClass = CameraSurfaceViewFragment.class;
                            break;
                        case WHICH_FRAGMENT_TEXTURE_VIEW:
                            fragmentClass = CameraTextureViewFragment.class;
                            break;
                        default:
                            throw new RuntimeException("Unexpected fragment type:" + whichFragment);
                    }
                    ft.add(R.id.activity_fragment_holder, fragmentClass, null);
                    ft.commit();
                }
            } else {
                finish();
            }
        }
    }

    @NonNull
    @Override
    protected HudCallbacks getCallbacks() {
        return new HudCallbacks(Looper.getMainLooper()) {
            IHudService mHmdService;

            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
            }

            @Override
            public void onConnected(boolean connected) throws RemoteException {
                if (connected && mHmdService != null) {
                    if (!mHmdService.hasCamera()) {
                        Toast.makeText(CameraActivity.this, "The attached device does not have a camera.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        };
    }
}

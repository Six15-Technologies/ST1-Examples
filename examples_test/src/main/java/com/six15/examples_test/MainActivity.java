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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudCompatActivity;
import com.six15.examples_test.presentation.BackgroundPresentingActivity;
import com.six15.examples_test.presentation.PresentingActivity;
import com.six15.examples_test.screen_mirroring.ScreenMirroringActivity;
import com.six15.examples_test.static_image.StaticImageActivity;
import com.six15.examples_test.voice_to_text.VoiceToTextActivity;
import com.six15.hudservice.IHudService;

public class MainActivity extends HudCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ViewGroup mParentView;
    private CoordinatorLayout mCoordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mParentView = findViewById(R.id.activity_main_parent);
        mCoordinatorLayout = findViewById(R.id.activity_main_coordinator);
    }

    @Override
    protected @NonNull
    HudCallbacks getCallbacks() {
        return new HudCallbacks() {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                if (!available) {
                    if (launchIntentForPermissions != null) {
                        //Launch this intent to request that the app be installed.
                        showPermissionSnack(launchIntentForPermissions);
                    } else {
                        //Ask the user to install the service app.
                        showAppNotInstalledSnack();
                    }
                    setAllViewsEnabled(false);
                } else {
                    setAllViewsEnabled(true);
                }
            }
        };
    }

    private void setAllViewsEnabled(boolean enabled) {
        for (int i = 0; i < mParentView.getChildCount(); i++) {
            View child = mParentView.getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    private void showAppNotInstalledSnack() {
        String requestString = "No service app installed. You must install the \"HMD Service\" or \"Six15 ST1\" app to connect to the ST1.";
        Snackbar.make(mCoordinatorLayout, requestString, BaseTransientBottomBar.LENGTH_INDEFINITE).show();
    }

    private void showPermissionSnack(Intent launchIntentForPermissions) {
        Log.i(TAG, "showSnackWithIntent: " + launchIntentForPermissions.getComponent());
        CharSequence appName = getPackageManager().resolveActivity(launchIntentForPermissions, 0).activityInfo.loadLabel(getPackageManager());
        CharSequence requestString = "Launch \"" + appName + "\" to request permissions";

        Snackbar.make(mCoordinatorLayout, requestString, BaseTransientBottomBar.LENGTH_INDEFINITE).setAction("Launch", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(launchIntentForPermissions);
                finish();
            }
        }).show();
    }

    private void startTestActivity(Class<? extends Activity> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }

    public void startStaticImageActivity(View view) {
        startTestActivity(StaticImageActivity.class);
    }

    public void startScreenMirroringActivity(View view) {
        startTestActivity(ScreenMirroringActivity.class);
    }

    public void startBackgroundPresentingActivity(View view) {
        startTestActivity(BackgroundPresentingActivity.class);
    }

    public void startIntentInterfaceActivity(View view) {
        startTestActivity(IntentInterfaceSelectionActivity.class);
    }

    public void startViewMirroringAndRendering(View view) {
        startTestActivity(ViewMirroringAndRenderingSelectionActivity.class);
    }

    public void startPresentingActivity(View view) {
        startTestActivity(PresentingActivity.class);
    }

    public void startCameraActivity(View view) {
        startTestActivity(CameraSelectionActivity.class);
    }

    public void startVoiceToTextActivity(View view) {
        startTestActivity(VoiceToTextActivity.class);
    }

}
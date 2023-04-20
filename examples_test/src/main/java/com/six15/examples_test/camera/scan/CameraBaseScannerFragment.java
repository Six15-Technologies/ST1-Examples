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

package com.six15.examples_test.camera.scan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.slider.Slider;
import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudRetainedFragment;
import com.six15.examples.helpers.HudCameraHelper;
import com.six15.examples.helpers.HudViewRenderingHelper;
import com.six15.examples_test.R;
import com.six15.hudservice.CameraResolution;
import com.six15.hudservice.IHudService;

import java.util.List;
import java.util.Objects;

public abstract class CameraBaseScannerFragment extends HudRetainedFragment {
    private static final String TAG = CameraBaseScannerFragment.class.getSimpleName();
    private static final int INITIAL_FOCUS = 25;
    private static final boolean INITIAL_AF_VALUE = false;
    private static final boolean ALLOW_MF = false;

    @Nullable
    private ImageView mCameraImageView;
    private HudCameraHelper mCameraHelper;
    private CameraResolution mVideoRes;
    @Nullable
    private Bitmap mCurrentBitmap = null;
    private ImageView mOverlayView;
    @Nullable
    private Bitmap mOverlayBitmap = null;
    private Paint mOverlayPaint;
    protected Handler mMainHandler;
    private HudViewRenderingHelper mHudViewRenderingHelper;
    private AppCompatTextView mHudBarcodeText;
    private String mCurrentTextOnHud = null;
    private IHudService mHmdService;
    private boolean mHudConnected = false;
    private TextView mPhoneBarcodeTextView;
    private static final int AF_ENABLE_DELAY = 5;
    private int mFramesLeftUntilChangeAF = AF_ENABLE_DELAY;
    private Slider mFocusSlider;
    private int mCurrentFocusSliderValue = -1;
    private boolean mCurrentFocus_AFValue = INITIAL_AF_VALUE;
    private SwitchCompat mFocusToggle;
    private boolean mAllowBitmapRecycle;


    protected void updateBarcodeUi(@Nullable String barcode_text, @Nullable Path path, int width, int height) {
        if (mOverlayView == null) {
            Log.i(TAG, "updateBarcodeUi: No view");
            return;
        }
        if (mOverlayBitmap == null || mOverlayBitmap.getHeight() != height || mOverlayBitmap.getWidth() != width) {
            mOverlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        mOverlayBitmap.eraseColor(Color.TRANSPARENT);

        if (path != null) {
            Canvas drawing = new Canvas(mOverlayBitmap);
            drawing.drawPath(path, mOverlayPaint);
        } else {
//            Log.i(TAG, "onBarcodeScanned: no barcode");
        }
        if (!TextUtils.isEmpty(barcode_text)) {
            mMainHandler.removeCallbacks(mClearTextRunnable);
            mMainHandler.postDelayed(mClearTextRunnable, 500);
            mPhoneBarcodeTextView.setText(barcode_text);
            sendTextToHud(barcode_text);
        } else {
            //Instead of clearing the HUD and phone UI on empty strings, schedule a clear 500ms later.
            //This stops flickering.
        }
        mOverlayView.setImageBitmap(mOverlayBitmap);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainHandler = new Handler(Looper.getMainLooper());

        mOverlayPaint = new Paint();
        mOverlayPaint.setColor(Color.RED);
        mOverlayPaint.setStrokeWidth(4);
        mOverlayPaint.setStyle(Paint.Style.STROKE);

        mHudViewRenderingHelper = new HudViewRenderingHelper(requireContext(), R.layout.hud_camera_scanner, R.style.Theme_HUD, null);
        mHudBarcodeText = mHudViewRenderingHelper.getView().findViewById(R.id.hud_camera_scanner);

        mCameraHelper = new HudCameraHelper(requireContext(), true, new HudCameraHelper.Callbacks() {

            @Nullable
            @Override
            protected CameraResolution getCameraResolution(@NonNull List<CameraResolution> resolutions) {
//                final int DESIRED_HEIGHT = 1080;//1280x720 maxZoom=1
//                final int DESIRED_HEIGHT = 720;//1280x720 maxZoom=1
                final int DESIRED_HEIGHT = 480;//640x480 maxZoom=6
                for (CameraResolution res : resolutions) {
                    if (res.getHeight() == DESIRED_HEIGHT) {
                        mVideoRes = res;
                        break;
                    }
                }
                mVideoRes = mVideoRes != null ? mVideoRes : super.getCameraResolution(resolutions);
                updateAspectRatioIfReady();
                return mVideoRes;
            }

            @Override
            protected boolean onCameraBitmap(@Nullable Bitmap bitmap) {
                super.onCameraBitmap(bitmap);
                if (mFramesLeftUntilChangeAF == 0 && mHmdService != null) {
                    try {
                        mHmdService.setCameraAutofocusMode(false);
//                        int zoom = mVideoRes.getMaxZoom();
                        int zoom = Math.min(mVideoRes.getMaxZoom(), 1);
                        Log.i(TAG, "onCameraBitmap: zoom:" + zoom);
                        //In some cases, setting the zoom to -1 helps force the camera to realize zoom changed.
                        mHmdService.setCameraZoom(-1);
                        mHmdService.setCameraZoom(zoom);
                        if (mCurrentFocus_AFValue == false) {
                            if (mCurrentFocusSliderValue == -1) {
                                mCurrentFocusSliderValue = INITIAL_FOCUS;
                            }
                        } else {
                            mCurrentFocusSliderValue = -1;
                        }
                        if (mFocusSlider != null) {
                            mFocusSlider.setValue(mCurrentFocusSliderValue);
                        }
                        sendFocusValueIfAble();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                if (mFramesLeftUntilChangeAF >= 0) {
                    mFramesLeftUntilChangeAF--;
                }
                mCurrentBitmap = bitmap;
                if (mCameraImageView != null) {
                    mCameraImageView.setImageBitmap(bitmap);
                    if (bitmap != null) {
                        processCameraBitmap(bitmap);
                    }
                }
                return mAllowBitmapRecycle;
            }
        });
        mAllowBitmapRecycle = allowBitmapRecycle();
        mCameraHelper.connect();

    }

    protected abstract void processCameraBitmap(@NonNull Bitmap bitmap);

    //Manually recycling bitmaps dramatically lowers average and peak memory usage.
    protected abstract boolean allowBitmapRecycle();

    protected abstract String getScannerName();

    @Override
    public void onPause() {
        super.onPause();
        //This works with the screen off, so there really is no reason to disable here.
//        mBarcodeCapture.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
//        mBarcodeCapture.setEnabled(true);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mCameraHelper.stopCamera();
        mCameraHelper.disconnect();
    }

    @NonNull
    @Override
    protected HudCallbacks getCallbacks() {
        return new HudCallbacks(Looper.getMainLooper()) {
            @Override
            public void onServiceConnectionChanged(boolean available, @Nullable IHudService hmdService, @Nullable Intent launchIntentForPermissions) {
                mHmdService = hmdService;
            }

            @Override
            public void onConnected(boolean connected) throws RemoteException {
                super.onConnected(connected);
                if (!connected) {
                    mCurrentTextOnHud = null;
                    mFramesLeftUntilChangeAF = AF_ENABLE_DELAY;
                }
                mHudConnected = connected;
            }
        };
    }

    private void sendTextToHud(@Nullable String barcode_text) {
        if (Objects.equals(barcode_text, mCurrentTextOnHud)) {
            return;
        }
        if (!mHudConnected) {
            return;
        }

        int max_lines = 1;
        if (barcode_text != null) {
            if (barcode_text.contains(" ") || barcode_text.contains("\n")) {
                max_lines = 2;
            }
        }
        int old_max = mHudBarcodeText.getMaxLines();
        if (old_max != max_lines) {
            //Setting max lines can leak memory that the GC needs to cleanup. Don't change it unless needed.
            mHudBarcodeText.setMaxLines(max_lines);
        }
        mHudBarcodeText.setText(barcode_text);
        mHudViewRenderingHelper.draw(mHmdService);
        mCurrentTextOnHud = barcode_text;
    }

    private final Runnable mClearTextRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPhoneBarcodeTextView != null) {
                mPhoneBarcodeTextView.setText(null);
            }
            if (mHmdService == null) {
                return;
            }
            if (!mHudConnected) {
                return;
            }
            mCurrentTextOnHud = null;
            try {
                mHmdService.clearHudDisplay();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    private void updateAspectRatioIfReady() {
        if (mCameraImageView == null || mVideoRes == null) {
            return;
        }
        int width = mVideoRes.getWidth();
        int height = mVideoRes.getHeight();
        String dimensionRatio = width + ":" + height;

        ViewGroup.LayoutParams params = mCameraImageView.getLayoutParams();
        if (params instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams constraint_params = (ConstraintLayout.LayoutParams) params;
            constraint_params.dimensionRatio = dimensionRatio;
            mCameraImageView.setLayoutParams(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_camera_base_scanner, container, false);
        mOverlayView = rootView.findViewById(R.id.fragment_camera_base_scanner_image_overlay);
        mPhoneBarcodeTextView = rootView.findViewById(R.id.fragment_camera_base_scanner_barcode_text);
        mCameraImageView = rootView.findViewById(R.id.fragment_camera_base_scanner_image);
        mCameraImageView.setImageBitmap(mCurrentBitmap);
        TextView nameView = rootView.findViewById(R.id.fragment_camera_base_scanner_name);
        nameView.setText(getScannerName());
        mFocusSlider = rootView.findViewById(R.id.fragment_camera_base_scanner_focus_slider);
        mFocusToggle = rootView.findViewById(R.id.fragment_camera_base_scanner_focus_switch);
        mFocusSlider.setVisibility(ALLOW_MF ? View.VISIBLE : View.INVISIBLE);

        mFocusSlider.setEnabled(!mCurrentFocus_AFValue);
        mFocusToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = mFocusToggle.isChecked();
                mCurrentFocus_AFValue = checked;
                mFocusSlider.setEnabled(!checked);
                sendFocusValueIfAble();
            }
        });
        if (mCurrentFocusSliderValue != -1) {
            mFocusSlider.setValue(mCurrentFocusSliderValue);
        }
        mFocusSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                if (mCurrentFocus_AFValue) {
                    //Can't manual focus in AF.
                    return;
                }
                mCurrentFocusSliderValue = (int) value;
                sendFocusValueIfAble();
            }
        });
        updateAspectRatioIfReady();
        return rootView;
    }

    private void sendFocusValueIfAble() {
        if (mHmdService == null) {
            return;
        }
        if (!mHudConnected) {
            return;
        }
        Log.i(TAG, "sendFocusValueIfAble: focusValue:" + mCurrentFocusSliderValue);
        mCameraHelper.requestAutoFocusEnabled(mCurrentFocus_AFValue);
//        try {
//            if (ALLOW_MF) {
//                if (!mCurrentFocus_AFValue) {
//                    mHmdService.setCameraFocus(mCurrentFocusSliderValue);
//                }
//            }
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mOverlayView = null;
        mPhoneBarcodeTextView = null;
        mCameraImageView = null;
    }
}

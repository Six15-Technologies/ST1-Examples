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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.scandit.datacapture.barcode.capture.BarcodeCapture;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureListener;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureSession;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureSettings;
import com.scandit.datacapture.barcode.capture.SymbologySettings;
import com.scandit.datacapture.barcode.data.Barcode;
import com.scandit.datacapture.barcode.data.Symbology;
import com.scandit.datacapture.barcode.feedback.BarcodeCaptureFeedback;
import com.scandit.datacapture.core.capture.DataCaptureContext;
import com.scandit.datacapture.core.common.feedback.Feedback;
import com.scandit.datacapture.core.common.feedback.Vibration;
import com.scandit.datacapture.core.common.geometry.Point;
import com.scandit.datacapture.core.common.geometry.Quadrilateral;
import com.scandit.datacapture.core.data.FrameData;
import com.scandit.datacapture.core.source.BitmapFrameSource;
import com.scandit.datacapture.core.source.FrameSourceState;
import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudRetainedFragment;
import com.six15.examples.helpers.HudCameraHelper;
import com.six15.examples.helpers.HudViewRenderingHelper;
import com.six15.examples_test.BuildConfig;
import com.six15.examples_test.R;
import com.six15.hudservice.CameraResolution;
import com.six15.hudservice.IHudService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class CameraScanditFragment extends HudRetainedFragment {
    private static final String TAG = CameraScanditFragment.class.getSimpleName();
    @Nullable
    private ImageView mCameraImageView;
    private HudCameraHelper mCameraHelper;
    private CameraResolution mVideoRes;
    @Nullable
    private Bitmap mCurrentBitmap = null;
    private DataCaptureContext mDataCaptureContext;
    private BarcodeCapture mBarcodeCapture;
    private ImageView mOverlayView;
    @Nullable
    private Bitmap mOverlayBitmap = null;
    private Paint mOverlayPaint;
    private Handler mMainHandler;
    private HudViewRenderingHelper mHudViewRenderingHelper;
    private AppCompatTextView mHudBarcodeText;
    private String mCurrentTextOnHud = null;
    private IHudService mHmdService;
    private boolean mHudConnected = false;
    private TextView mPhoneBarcodeTextView;
    private static final int AF_ENABLE_DELAY = 5;
    private int mFramesLeftUntilChangeAF = AF_ENABLE_DELAY;

    private BarcodeCaptureListener mBarcodeListener = new BarcodeCaptureListener() {

        @Override
        public void onBarcodeScanned(@NonNull BarcodeCapture barcodeCapture, @NonNull BarcodeCaptureSession barcodeCaptureSession, @NonNull FrameData frameData) {
        }

        @Override
        public void onSessionUpdated(@NonNull BarcodeCapture barcodeCapture, @NonNull BarcodeCaptureSession barcodeCaptureSession, @NonNull FrameData frameData) {
            int width = frameData.getImageBuffer().getWidth();
            int height = frameData.getImageBuffer().getHeight();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateOverlayAndHudWithBarcode(barcodeCaptureSession, width, height);
                }
            });
        }

        @Override
        public void onObservationStarted(@NonNull BarcodeCapture barcodeCapture) {
        }

        @Override
        public void onObservationStopped(@NonNull BarcodeCapture barcodeCapture) {
        }
    };


    private void updateOverlayAndHudWithBarcode(BarcodeCaptureSession barcodeCaptureSession, int width, int height) {
        if (mOverlayView == null) {
            Log.i(TAG, "onBarcodeScanned: No view");
            return;
        }
        if (mOverlayBitmap == null || mOverlayBitmap.getHeight() != height || mOverlayBitmap.getWidth() != width) {
            mOverlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        mOverlayBitmap.eraseColor(Color.TRANSPARENT);

        String barcode_text = null;
        List<Barcode> barcodes = barcodeCaptureSession.getNewlyRecognizedBarcodes();
        if (!barcodes.isEmpty()) {
            Barcode barcode = barcodes.get(0);
            Quadrilateral location = barcode.getLocation();
            barcode_text = barcode.getData();
            Log.i(TAG, "updateOverlayWithBarcode: barcode_text: " + barcode_text);

            Canvas drawing = new Canvas(mOverlayBitmap);
            Path path = new Path();
            Point tl = location.getTopLeft();
            Point br = location.getBottomRight();
            path.moveTo(tl.getX(), tl.getY());
            path.lineTo(br.getX(), tl.getY());
            path.lineTo(br.getX(), br.getY());
            path.lineTo(tl.getX(), br.getY());
            path.lineTo(tl.getX(), tl.getY());
            drawing.drawPath(path, mOverlayPaint);
        } else {
//            Log.i(TAG, "onBarcodeScanned: no barcode");
        }
        mPhoneBarcodeTextView.setText(barcode_text);
        if (!TextUtils.isEmpty(barcode_text)) {
            mMainHandler.removeCallbacks(mClearHudRunnable);
            mMainHandler.postDelayed(mClearHudRunnable, 500);
            sendTextToHud(barcode_text);
        } else {
            //Instead of clearing the HUD on empty strings, schedule a clear 500ms later.
            //This stops flickering.
            //The UI on the phone still flickers. They could use similar logic, but don't right now.
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

        mHudViewRenderingHelper = new HudViewRenderingHelper(requireContext(), R.layout.hud_camera_scandit, R.style.Theme_HUD, null);
        mHudBarcodeText = mHudViewRenderingHelper.getView().findViewById(R.id.hud_camera_scandit_text);

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
                Log.i(TAG, "getCameraResolution: res:" + mVideoRes.getWidth() + "x" + mVideoRes.getHeight());
                updateAspectRatioIfReady();
                return mVideoRes;
            }

            @Override
            protected boolean onCameraBitmap(@Nullable Bitmap bitmap) {
                super.onCameraBitmap(bitmap);
                if (mFramesLeftUntilChangeAF == 0) {
                    try {
                        mHmdService.setCameraAutofocusMode(true);
//                        int zoom = mVideoRes.getMaxZoom();
                        int zoom = Math.min(mVideoRes.getMaxZoom(), 1);
                        Log.i(TAG, "onCameraBitmap: zoom:" + zoom);
                        //In some cases, setting the zoom to -1 helps force the camera to realize zoom changed.
                        mHmdService.setCameraZoom(-1);
                        mHmdService.setCameraZoom(zoom);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                if (mFramesLeftUntilChangeAF >= 0) {
                    mFramesLeftUntilChangeAF--;
                }
                mCurrentBitmap = bitmap;
                if (mCameraImageView != null) {
                    if (bitmap != null) {
                        //BitmapFrameSource leaks tons of memory that the garbage collector has to clean up.
                        BitmapFrameSource bfs = BitmapFrameSource.of(bitmap);
                        mDataCaptureContext.setFrameSource(bfs);
                        bfs.switchToDesiredState(FrameSourceState.ON, null);
                    }
                    mCameraImageView.setImageBitmap(bitmap);
                }
                //The BitmapFrameSource seems like it creates a copy, so we're free to return true and allow HudCameraHelper
                //to release our old instances.
                return true;
            }
        });
        mCameraHelper.connect();

        setupScandit();

    }

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

    private void setupScandit() {
        if (BuildConfig.SCANDIT_LICENSE_KEY.equals("null")) {
            Toast.makeText(requireContext(), "No SCANDIT_LICENSE_KEY. Add one to local.properties", Toast.LENGTH_LONG).show();
        }
        mDataCaptureContext = DataCaptureContext.forLicenseKey(BuildConfig.SCANDIT_LICENSE_KEY);

        // The barcode capturing process is configured through barcode capture settings
        // which are then applied to the barcode capture instance that manages barcode recognition.
        BarcodeCaptureSettings barcodeCaptureSettings = new BarcodeCaptureSettings();

        // The settings instance initially has all types of barcodes (symbologies) disabled.
        // For the purpose of this sample we enable a very generous set of symbologies.
        // In your own app ensure that you only enable the symbologies that your app requires as
        // every additional enabled symbology has an impact on processing times.
        HashSet<Symbology> symbologies = new HashSet<>();
        symbologies.add(Symbology.EAN13_UPCA);
        symbologies.add(Symbology.EAN8);
        symbologies.add(Symbology.UPCE);
        symbologies.add(Symbology.QR);
        symbologies.add(Symbology.DATA_MATRIX);
        symbologies.add(Symbology.CODE39);
        symbologies.add(Symbology.CODE128);
        symbologies.add(Symbology.INTERLEAVED_TWO_OF_FIVE);

        barcodeCaptureSettings.enableSymbologies(symbologies);

        // Some linear/1d barcode symbologies allow you to encode variable-length data.
        // By default, the Scandit Data Capture SDK only scans barcodes in a certain length range.
        // If your application requires scanning of one of these symbologies, and the length is
        // falling outside the default range, you may need to adjust the "active symbol counts"
        // for this symbology. This is shown in the following few lines of code for one of the
        // variable-length symbologies.
        SymbologySettings symbologySettings = barcodeCaptureSettings.getSymbologySettings(Symbology.CODE39);

        HashSet<Short> activeSymbolCounts = new HashSet<>(Arrays.asList(new Short[]{7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}));

        symbologySettings.setActiveSymbolCounts(activeSymbolCounts);

        // Create new barcode capture mode with the settings from above.
        mBarcodeCapture = BarcodeCapture.forDataCaptureContext(mDataCaptureContext, barcodeCaptureSettings);

        BarcodeCaptureFeedback feedback = new BarcodeCaptureFeedback();
        boolean enableVibrate = false;
        Feedback success_feedback = new Feedback(enableVibrate ? Vibration.defaultVibration() : null, feedback.getSuccess().getSound());
        feedback.setSuccess(success_feedback);
        mBarcodeCapture.setFeedback(feedback);
        // Register self as a listener to get informed whenever a new barcode got recognized.
        mBarcodeCapture.addListener(mBarcodeListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCameraHelper.stopCamera();
        mBarcodeCapture.removeListener(mBarcodeListener);
        mDataCaptureContext.removeMode(mBarcodeCapture);
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

    private final Runnable mClearHudRunnable = new Runnable() {
        @Override
        public void run() {
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
        View rootView = inflater.inflate(R.layout.fragment_camera_scandit, container, false);
        mOverlayView = rootView.findViewById(R.id.fragment_camera_scandit_image_overlay);
        mPhoneBarcodeTextView = rootView.findViewById(R.id.fragment_camera_scandit_barcode_text);
        mCameraImageView = rootView.findViewById(R.id.fragment_camera_scandit_image);
        mCameraImageView.setImageBitmap(mCurrentBitmap);
        updateAspectRatioIfReady();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mOverlayView = null;
        mPhoneBarcodeTextView = null;
        mCameraImageView = null;
    }
}

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

import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.six15.examples_test.BuildConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class CameraScanditFragment extends CameraBaseScannerFragment {
    private static final String TAG = CameraScanditFragment.class.getSimpleName();

    private DataCaptureContext mDataCaptureContext;
    private BarcodeCapture mBarcodeCapture;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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
        super.onCreate(savedInstanceState);
    }

    private BarcodeCaptureListener mBarcodeListener = new BarcodeCaptureListener() {

        @Override
        public void onBarcodeScanned(@NonNull BarcodeCapture barcodeCapture, @NonNull BarcodeCaptureSession barcodeCaptureSession, @NonNull FrameData frameData) {
        }

        @Override
        public void onSessionUpdated(@NonNull BarcodeCapture barcodeCapture, @NonNull BarcodeCaptureSession barcodeCaptureSession, @NonNull FrameData frameData) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    int width = frameData.getImageBuffer().getWidth();
                    int height = frameData.getImageBuffer().getHeight();
                    String barcode_text = null;
                    Path path = null;
                    List<Barcode> barcodes = barcodeCaptureSession.getNewlyRecognizedBarcodes();
                    if (!barcodes.isEmpty()) {
                        Barcode barcode = barcodes.get(0);
                        Quadrilateral location = barcode.getLocation();
                        barcode_text = barcode.getData();
                        Log.i(TAG, "updateOverlayWithBarcode: barcode_text: " + barcode_text);

                        Point tl = location.getTopLeft();
                        Point tr = location.getTopRight();
                        Point bl = location.getBottomLeft();
                        Point br = location.getBottomRight();

                        path = new Path();
                        path.moveTo(tl.getX(), tl.getY());
                        path.lineTo(tr.getX(), tr.getY());
                        path.lineTo(br.getX(), br.getY());
                        path.lineTo(bl.getX(), bl.getY());
                        path.lineTo(tl.getX(), tl.getY());
                    } else {
//                        Log.i(TAG, "onBarcodeScanned: no barcode");
                    }
                    updateBarcodeUi(barcode_text, path, width, height);
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

    @Override
    protected void processCameraBitmap(@NonNull Bitmap bitmap) {
        //BitmapFrameSource uses tons of memory that the garbage collector has to clean up, but it's fine.
        BitmapFrameSource bfs = BitmapFrameSource.of(bitmap);
        mDataCaptureContext.setFrameSource(bfs);
        bfs.switchToDesiredState(FrameSourceState.ON, null);
    }

    @Override
    protected boolean allowBitmapRecycle() {
        //BitmapFrameSource seems like it creates a copy, so we can allow recycling.
        return true;
    }

    @Override
    protected String getScannerName() {
        return "Scandit";
    }

    @Override
    public void onDestroy() {
        mBarcodeCapture.removeListener(mBarcodeListener);
        mDataCaptureContext.removeMode(mBarcodeCapture);
        super.onDestroy();
    }


}

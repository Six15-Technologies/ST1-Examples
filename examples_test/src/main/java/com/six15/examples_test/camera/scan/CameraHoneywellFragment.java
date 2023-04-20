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
import android.graphics.Point;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.honeywell.barcode.BarcodeBounds;
import com.honeywell.barcode.HSMDecodeResult;
import com.honeywell.barcode.HSMDecoder;
import com.honeywell.barcode.Symbology;
import com.honeywell.license.ActivationManager;
import com.honeywell.license.ActivationResult;
import com.six15.examples_test.BuildConfig;

public class CameraHoneywellFragment extends CameraBaseScannerFragment {
    private static final String TAG = CameraHoneywellFragment.class.getSimpleName();

    @Nullable
    private HSMDecoder mHsmDecoder;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (BuildConfig.HONEYWELL_LICENSE_ID.equals("null")) {
            Toast.makeText(requireContext(), "No HONEYWELL_LICENSE_ID. Add one to local.properties", Toast.LENGTH_LONG).show();
        }
        ActivationResult activationResult = ActivationManager.activateEntitlement(requireContext(), BuildConfig.HONEYWELL_LICENSE_ID);
        int activationResultValue = activationResult.getValue();
        if (activationResultValue != ActivationResult.SUCCESS.getValue()) {
            Toast.makeText(requireContext(), "Honeywell Activation Result: " + activationResult, Toast.LENGTH_LONG).show();
        }

        mHsmDecoder = HSMDecoder.getInstance(requireContext());

        //set all decoder related settings
        mHsmDecoder.enableSymbology(Symbology.UPCA);
        mHsmDecoder.enableSymbology(Symbology.CODE128);
        mHsmDecoder.enableSymbology(Symbology.CODE39);
        mHsmDecoder.enableSymbology(Symbology.QR);

        super.onCreate(savedInstanceState);
    }


    @Override
    protected void processCameraBitmap(@NonNull Bitmap bitmap) {
        if (mHsmDecoder != null) {
            byte[] bytes = bitmap_to_bytes_GRAY(bitmap);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            HSMDecodeResult[] results = mHsmDecoder.decodeImage(bytes, width, height, bitmap.getWidth());
            String barcode_text = null;
            Path path = null;
            if (results.length > 0) {
                HSMDecodeResult firstResult = results[0];
                barcode_text = firstResult.getBarcodeData();
                BarcodeBounds bounds = firstResult.getBarcodeBounds();
                Point tl = bounds.getTopLeft();
                Point tr = bounds.getTopRight();
                Point bl = bounds.getBottomLeft();
                Point br = bounds.getBottomRight();
                path = new Path();
                path.moveTo(tl.x, tl.y);
                path.lineTo(tr.x, tr.y);
                path.lineTo(br.x, br.y);
                path.lineTo(bl.x, bl.y);
                path.lineTo(tl.x, tl.y);

            }
            updateBarcodeUi(barcode_text, path, width, height);
        }
    }

    @Override
    protected boolean allowBitmapRecycle() {
        //We must make a copy to convert the Bitmap to grayscale. Therefore recycling the source bitmap is fine.
        return true;
    }

    @Override
    protected String getScannerName() {
        return "Honeywell";
    }

    private int[] cached_pixels = new int[1];
    private byte[] cached_bytes = new byte[1];
    private byte[] bitmap_to_bytes_GRAY(@NonNull Bitmap bitmap) {
        int size = bitmap.getWidth() * bitmap.getHeight();
//        if (size != cached_bytes.length) {
            cached_pixels = new int[size];
            cached_bytes = new byte[size];
//        }
        bitmap.getPixels(cached_pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int i = 0;
        for (int pixel : cached_pixels) {
//            float A = (pixel >> 24) & 0xff;
            float R = (pixel >> 16) & 0xff;
            float G = (pixel >> 8) & 0xff;
            float B = pixel & 0xff;
            float lum = 0.3f * R + 0.59f * G + B;
            lum = Math.min(lum, 255.0f);
            lum = Math.max(lum, 0.0f);
            cached_bytes[i++] = (byte) lum;
        }
        return cached_bytes;
    }

    @Override
    public void onDestroy() {
        HSMDecoder.disposeInstance();
        super.onDestroy();
    }
}

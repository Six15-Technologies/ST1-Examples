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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class CameraML_KitFragment extends CameraBaseScannerFragment {
    private static final String TAG = CameraML_KitFragment.class.getSimpleName();
    private BarcodeScanner mScanner;
    private Task<List<Barcode>> mTask;
    private Bitmap mProcessingBitmap;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_UNKNOWN
        ).build();
        mScanner = BarcodeScanning.getClient(options);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void processCameraBitmap(@NonNull Bitmap bitmap) {
        if (mTask != null) {
            return;
        }
        if (mProcessingBitmap != null) {
            //Dropped bitmaps (when Task != null) are not recycled.
            //This doesn't seem to happen often.
            //Manually recycling those bitmaps we can is better than letting the GC clean them up later and dramatically lowers average and peak memory usage.
            mProcessingBitmap.recycle();
        }
        mProcessingBitmap = bitmap;
        mTask = mScanner.process(InputImage.fromBitmap(bitmap, 0));
        mTask.addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(List<Barcode> barcodes) {
                String text = null;
                Point[] cornerPoints = null;
                Path path = null;
                if (barcodes.size() > 0) {
                    Barcode barcode = barcodes.get(0);
                    text = barcode.getRawValue();
                    cornerPoints = barcode.getCornerPoints();
                    if (cornerPoints != null) {
                        path = new Path();
                        Point tl = cornerPoints[0];
                        Point tr = cornerPoints[1];
                        Point br = cornerPoints[2];
                        Point bl = cornerPoints[3];
                        path.moveTo(tl.x, tl.y);
                        path.lineTo(tr.x, tr.y);
                        path.lineTo(br.x, br.y);
                        path.lineTo(bl.x, bl.y);
                        path.lineTo(tl.x, tl.y);
                    }
                }
                Log.i(TAG, "onSuccess: path:" + path);
                if (path != null){
                    Log.i(TAG, "onSuccess: Path");
                }
                updateBarcodeUi(text, path, bitmap.getWidth(), bitmap.getHeight());
            }
        });
        mTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure() called with: e = [" + e + "]");
            }
        });
        mTask.addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
            @Override
            public void onComplete(@NonNull Task<List<Barcode>> task) {
                mTask = null;
            }
        });
    }

    @Override
    protected boolean allowBitmapRecycle() {
        //ML Kit doesn't immediately create a copy, so we can't recycle.
        return false;
    }

    @Override
    protected String getScannerName() {
        return "ML Kit";
    }

    @Override
    public void onDestroy() {
        mScanner.close();
        super.onDestroy();
    }
}

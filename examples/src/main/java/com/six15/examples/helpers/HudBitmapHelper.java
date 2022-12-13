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

package com.six15.examples.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.hudservice.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HudBitmapHelper {
    private static final int MODE_CENTER_BLACK_BAR = 0;
    private static final int MODE_CENTER_CROP = 1;
    private static final int MODE_TOP_LEFT_CROP = 2;
    private static final int MODE_BOTTOM_RIGHT_CROP = 3;
    private static final int MODE_CROP_WITH_RECT = 4;

    private static final boolean ALLOW_BLACK_BAR_CROP = true;

    //Scale and crop a bitmap to HUD size such that, of the valid pixels rectInp, cropRect pixels are visible.
    //Black bars are added to maintain aspect ratio.
    public static Bitmap calculateAdjustedBitmap(@NonNull Bitmap bmpInp) {
       return calculateAdjustedBitmap(bmpInp, null, null, new Paint());
    }

    public static Bitmap calculateAdjustedBitmap(@NonNull Bitmap bmpInp, @Nullable Rect rectInp, @Nullable Rect cropRect, @NonNull Paint paint) {

        if (rectInp == null) {
            rectInp = new Rect(0, 0, bmpInp.getWidth(), bmpInp.getHeight());
        }

        int mode;
        if (cropRect == null || cropRect.width() <= 0 || cropRect.height() <= 0) {
            mode = MODE_CENTER_BLACK_BAR;
        } else {
            mode = MODE_CROP_WITH_RECT;
        }
//        mode = MODE_CENTER_CROP;
//        mode = MODE_TOP_LEFT_CROP;
//        mode = MODE_BOTTOM_RIGHT_CROP;

        if (ALLOW_BLACK_BAR_CROP) {
            if (mode == MODE_CROP_WITH_RECT) {
                final Bitmap croppedBitmap = Bitmap.createBitmap(cropRect.width(), cropRect.height(), Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(croppedBitmap);
                rectInp = new Rect(0, 0, cropRect.width(), cropRect.height());
                c.drawBitmap(bmpInp, cropRect, rectInp, paint);
                bmpInp = croppedBitmap;

                //
                mode = MODE_CENTER_BLACK_BAR;
            }
        } else {
            //Stretch crop
        }


        int desired_width = Constants.ST1_HUD_WIDTH;
        int desired_height = Constants.ST1_HUD_HEIGHT;
        float actual_width_f = rectInp.width();
        float actual_height_f = rectInp.height();

        Matrix correctionMatrix = new Matrix();

        float scale_w = ((float) desired_width) / actual_width_f;
        float scale_h = ((float) desired_height) / actual_height_f;

        float scale;
        if (mode == MODE_CENTER_BLACK_BAR) {
            scale = Math.min(scale_h, scale_w);
        } else {
            scale = Math.max(scale_h, scale_w);
        }

        float remaining_width = desired_width / scale - actual_width_f;
        float remaining_height = desired_height / scale - actual_height_f;

        float translate_x;
        float translate_y;

        if (mode == MODE_TOP_LEFT_CROP) {
            translate_x = 0.0f;
            translate_y = 0.0f;
        } else if (mode == MODE_BOTTOM_RIGHT_CROP) {
            translate_x = remaining_width;
            translate_y = remaining_height;
        } else if (mode == MODE_CROP_WITH_RECT) {
            translate_x = -cropRect.left;
            translate_y = -cropRect.top;
        } else {
            translate_x = remaining_width / 2.0f;
            translate_y = remaining_height / 2.0f;
        }

        correctionMatrix.postTranslate(translate_x, translate_y);
        if (mode == MODE_CROP_WITH_RECT) {
            correctionMatrix.postScale((float) desired_width / (float) cropRect.width(), (float) desired_height / (float) cropRect.height());
        } else {
            correctionMatrix.postScale(scale, scale);
        }


        final Bitmap correctedBitmap = Bitmap.createBitmap(desired_width, desired_height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(correctedBitmap);
        c.drawBitmap(bmpInp, correctionMatrix, paint);
        return correctedBitmap;
    }

    public static String saveBitmapAsJpeg(Context context, Bitmap bmp, int jpegQuality, String folder, String name) {
        return saveBitmapAsJpeg(context, bmp, jpegQuality, folder, name, false);
    }
    public static String saveBitmapAsJpeg(Context context, Bitmap bmp, int jpegQuality, String folder, String name, boolean prefer_external) {
        File baseDir = null;
        if (prefer_external) {
            baseDir = context.getExternalFilesDir(null);
        }
        if (baseDir == null){
            baseDir = context.getFilesDir();
        }
        String path = baseDir.getPath() + File.separator + folder + File.separator + name + ".jpg";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos);

        File file = new File(path);
        file.mkdirs();
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }
}

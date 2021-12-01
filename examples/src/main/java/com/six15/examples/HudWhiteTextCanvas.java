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

package com.six15.examples;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.text.MeasuredText;

import androidx.annotation.NonNull;

public class HudWhiteTextCanvas extends Canvas {
    private static final String TAG = HudWhiteTextCanvas.class.getSimpleName();

    public HudWhiteTextCanvas(Bitmap bitmap) {
        super(bitmap);
    }

    public static boolean isDarkColor(int colorIntValue, int alpha) {
        int red = Color.red(colorIntValue);
        int green = Color.green(colorIntValue);
        int blue = Color.blue(colorIntValue);
        double lum = ((0.299 * red) + (0.587 * green) + (0.114 * blue)) * (((double) alpha) / 255.0);
        return lum < 50;
    }

    private Paint adjustPaint(Paint paint) {
        int oldColor = paint.getColor();
        boolean isDark = isDarkColor(oldColor, paint.getAlpha());
        if (isDark) {
            paint = new Paint(paint);
            paint.setColor(Color.WHITE);
        }
        return paint;
    }

    @Override
    public void drawPosText(@NonNull char[] text, int index, int count, @NonNull float[] pos, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawPosText(text, index, count, pos, paint);
    }

    @Override
    public void drawPosText(@NonNull String text, @NonNull float[] pos, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawPosText(text, pos, paint);
    }

    @Override
    public void drawText(@NonNull char[] text, int index, int count, float x, float y, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawText(text, index, count, x, y, paint);
    }

    @Override
    public void drawText(@NonNull String text, float x, float y, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawText(text, x, y, paint);
    }

    @Override
    public void drawText(@NonNull String text, int start, int end, float x, float y, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawText(text, start, end, x, y, paint);
    }

    @Override
    public void drawText(@NonNull CharSequence text, int start, int end, float x, float y, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawText(text, start, end, x, y, paint);
    }

    @Override
    public void drawTextOnPath(@NonNull char[] text, int index, int count, @NonNull Path path, float hOffset, float vOffset, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawTextOnPath(text, index, count, path, hOffset, vOffset, paint);
    }

    @Override
    public void drawTextOnPath(@NonNull String text, @NonNull Path path, float hOffset, float vOffset, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawTextOnPath(text, path, hOffset, vOffset, paint);
    }

    @Override
    public void drawTextRun(@NonNull char[] text, int index, int count, int contextIndex, int contextCount, float x, float y, boolean isRtl, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawTextRun(text, index, count, contextIndex, contextCount, x, y, isRtl, paint);
    }

    @Override
    public void drawTextRun(@NonNull CharSequence text, int start, int end, int contextStart, int contextEnd, float x, float y, boolean isRtl, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint);
    }

    @Override
    public void drawTextRun(@NonNull MeasuredText text, int start, int end, int contextStart, int contextEnd, float x, float y, boolean isRtl, @NonNull Paint paint) {
        paint = adjustPaint(paint);
        super.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint);
    }
}

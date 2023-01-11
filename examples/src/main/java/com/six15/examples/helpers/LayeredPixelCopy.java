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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.O)
public class LayeredPixelCopy {
    private static final String TAG = LayeredPixelCopy.class.getSimpleName();
    private final boolean mOnlySurfaces;
    private Paint mPaint;

    private static class PerLayerInfo {
        public Bitmap bitmap;
        public int[] loc_xy;
        public int width;
        public int height;
        public View view;
        public boolean isSurfaceView;
    }

    private ArrayList<PerLayerInfo> mDrawingInfo = new ArrayList<>();

    public LayeredPixelCopy(boolean onlySurfaces) {
        mPaint = new Paint();
        mOnlySurfaces = onlySurfaces;
    }

    public LayeredPixelCopy() {
        this(false);
    }

    public boolean request(@NonNull View view, @NonNull Bitmap drawingBitmap, @NonNull PixelCopy.OnPixelCopyFinishedListener listener, @NonNull Handler backgroundHandler) {
        if (view.getWidth() != drawingBitmap.getWidth()) {
            Log.e(TAG, "request: Expecting matching widths: " + view.getWidth() + " " + drawingBitmap.getWidth());
            return false;
        }
        if (view.getHeight() != drawingBitmap.getHeight()) {
            Log.e(TAG, "request: Expecting matching heights: " + view.getHeight() + " " + drawingBitmap.getHeight());
            return false;
        }
        boolean foundSurfaceView = populateDrawingInfosFromView(view);
        triggerPixelCopies(drawingBitmap, backgroundHandler, listener);
//        return foundSurfaceView;
        return true;
    }

    private boolean populateDrawingInfosFromView(View rootView) {
        boolean foundSurfaceView = false;
        ArrayList<View> views = new ArrayList<View>(2);
        recursiveFindSurfaceViews(rootView, views);
        //Add the root view at the top view unless it's already the only surface view.
        if (views.size() == 0 || (views.get(0) != rootView)) {
            views.add(rootView);
        }
        for (int i = 0; i < views.size(); i++) {
            View view = views.get(i);
            PerLayerInfo info;
            if (mDrawingInfo.size() <= i) {
                info = new PerLayerInfo();
                mDrawingInfo.add(info);
            } else {
                info = mDrawingInfo.get(i);
            }
            info.height = view.getHeight();
            info.width = view.getWidth();
            info.view = view;
            info.isSurfaceView = view instanceof SurfaceView;
            foundSurfaceView |= info.isSurfaceView;
            info.loc_xy = new int[2];
            info.view.getLocationInWindow(info.loc_xy);

            if (info.bitmap == null || info.bitmap.getWidth() != info.width || info.bitmap.getHeight() != info.height) {
                if (info.bitmap != null) {
                    info.bitmap.recycle();
                    info.bitmap = null;
                }
                info.bitmap = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888);
            }
        }
        while (mDrawingInfo.size() > views.size()) {
            PerLayerInfo info = mDrawingInfo.get(mDrawingInfo.size() - 1);
            if (info.bitmap != null) {
                info.bitmap.recycle();
                info.bitmap = null;
            }
            mDrawingInfo.remove(mDrawingInfo.size() - 1);
        }
        return foundSurfaceView;
    }

    private void recursiveFindSurfaceViews(View view, ArrayList<View> result) {
        if (view instanceof SurfaceView) {
            result.add(view);
            return;
        }
        if (view instanceof ViewGroup) {
            int childCount = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = ((ViewGroup) view).getChildAt(i);
                recursiveFindSurfaceViews(child, result);
            }
        }
    }

    public static Activity tryToGetActivity(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return tryToGetActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    private void triggerPixelCopies(Bitmap finalBitmap, Handler backgroundHandler, PixelCopy.OnPixelCopyFinishedListener endListener) {
        final int totalCopies = mDrawingInfo.size();
        PixelCopy.OnPixelCopyFinishedListener listener = new PixelCopy.OnPixelCopyFinishedListener() {
            boolean hasError = false;
            int numFinished = 0;

            @Override
            public void onPixelCopyFinished(int copyResult) {
                numFinished++;
                if (copyResult == PixelCopy.SUCCESS) {
                    if (numFinished == totalCopies && !hasError) {
                        drawIntoFinalBitmap(finalBitmap);
                        endListener.onPixelCopyFinished(PixelCopy.SUCCESS);
                    }
                } else {
                    hasError = true;
                    endListener.onPixelCopyFinished(copyResult);
                }
            }
        };
        for (PerLayerInfo info : mDrawingInfo) {
            if (info.isSurfaceView) {
                SurfaceView surfaceView = (SurfaceView) info.view;
                if (surfaceView.getHolder().getSurface().isValid()) {
                    PixelCopy.request(surfaceView, info.bitmap, listener, backgroundHandler);
                } else {
                    Log.i(TAG, "triggerPixelCopies: Not valid!");
                    info.bitmap.eraseColor(Color.BLACK);
                    listener.onPixelCopyFinished(PixelCopy.SUCCESS);
                }
            } else if (mOnlySurfaces) {
                //Mark the draw as passed.
                info.bitmap.eraseColor(Color.BLACK);
                listener.onPixelCopyFinished(PixelCopy.SUCCESS);
            } else { // Ui
                Activity activity = tryToGetActivity(info.view.getContext());
                if (activity == null) {
                    listener.onPixelCopyFinished(PixelCopy.SUCCESS);
                    Log.i(TAG, "triggerPixelCopies: Can't find activity");
                }
                Rect rect = new Rect(info.loc_xy[0], info.loc_xy[1], info.loc_xy[0] + info.width, info.loc_xy[1] + info.height);
                PixelCopy.request(activity.getWindow(), rect, info.bitmap, listener, backgroundHandler);
            }
        }
    }

    private void drawIntoFinalBitmap(Bitmap finalBitmap) {
        if (finalBitmap.isRecycled()) {
            return;
        }
        Canvas drawingCanvas;
        try {
            drawingCanvas = new Canvas(finalBitmap);
        } catch (RuntimeException e) {
            return;
        }
        drawingCanvas.drawColor(Color.BLACK);
        if (mDrawingInfo.size() == 0) {
            //If we have no images simply clear the display and leave.
            return;
        }

        int[] loc_xy_main = mDrawingInfo.get(mDrawingInfo.size() - 1).loc_xy;
//        Log.i(TAG, "drawIntoFinalBitmap: Start");
        for (PerLayerInfo info : mDrawingInfo) {
            int x_offset = 0;
            int y_offset = 0;
            if (info.isSurfaceView) {
                x_offset = info.loc_xy[0] - loc_xy_main[0];
                y_offset = info.loc_xy[1] - loc_xy_main[1];
            } else if (mOnlySurfaces) {
                continue;
            }
            drawingCanvas.drawBitmap(info.bitmap, x_offset, y_offset, mPaint);
        }
    }
}

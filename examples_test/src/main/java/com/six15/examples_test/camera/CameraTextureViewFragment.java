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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.six15.examples.helpers.HudCameraHelper;
import com.six15.examples_test.R;
import com.six15.hudservice.CameraResolution;

import java.util.List;

public class CameraTextureViewFragment extends Fragment {
    //This class makes use of a Bitmap to draw the camera onto a SurfaceView.
    //We don't support inter process Surface rendering, like is done with the Camera1 and Camera2 API's.

    private static final String TAG = CameraTextureViewFragment.class.getSimpleName();
    private HudCameraHelper mCameraHelper;
    private CameraResolution mVideoRes;
    private TextureView mCameraTextureView;
    private Surface mSurface;
    private Bitmap mCurrentBitmap;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable() called with: surfaceTexture = [" + surfaceTexture + "], width = [" + width + "], height = [" + height + "]");
            mSurface = new Surface(surfaceTexture);
            if (mCurrentBitmap != null) {
                //Caching and drawing the previous bitmap can slightly reduce glitches during configuration changes.
                drawBitmapOntoSurface(mCurrentBitmap, mSurface);
            }
            mCameraHelper.startCameraIfReady();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged() called with: surface = [" + surface + "], width = [" + width + "], height = [" + height + "]");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureDestroyed() called with: surface = [" + surface + "]");
            mSurface = null;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mCameraHelper = new HudCameraHelper(requireContext(), true, new HudCameraHelper.Callbacks() {

            @Nullable
            @Override
            protected CameraResolution getCameraResolution(@NonNull List<CameraResolution> resolutions) {
                //Use the default resolution, but remember it so we can use it's aspect ratio.
                mVideoRes = super.getCameraResolution(resolutions);
                updateAspectRatioIfReady();
                return mVideoRes;
            }

            @Override
            protected boolean onCameraBitmap(@Nullable Bitmap bitmap) {
                super.onCameraBitmap(bitmap);
                mCurrentBitmap = bitmap;
                if (mSurface == null) {
                    Log.i(TAG, "onCameraBitmap: No surface");
                    return true;
                }
                if (bitmap == null) {
                    drawBlackOntoSurface(mSurface);
                } else {
                    drawBitmapOntoSurface(mCurrentBitmap, mSurface);
                }
                return true;
            }
        });
        mCameraHelper.connect();
    }

    private static void drawBitmapOntoSurface(@NonNull Bitmap bitmap, @NonNull Surface surface) {
        Canvas canvas = surface.lockCanvas(null);
        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect dest = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmap, src, dest, null);
        surface.unlockCanvasAndPost(canvas);
    }

    private static void drawBlackOntoSurface(@NonNull Surface surface) {
        Canvas canvas = surface.lockCanvas(null);
        canvas.drawColor(Color.BLACK);
        surface.unlockCanvasAndPost(canvas);
    }

    private void updateAspectRatioIfReady() {
        if (mCameraTextureView == null || mVideoRes == null) {
            return;
        }
        ViewGroup.LayoutParams params = mCameraTextureView.getLayoutParams();
        if (params instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams constraint_params = (ConstraintLayout.LayoutParams) params;
            int width = mVideoRes.getWidth();
            int height = mVideoRes.getHeight();
            constraint_params.dimensionRatio = width + ":" + height;
            mCameraTextureView.setLayoutParams(params);
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup
            container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() called with: inflater = [" + inflater + "], container = [" + container + "], savedInstanceState = [" + savedInstanceState + "]");
        View rootView = inflater.inflate(R.layout.fragment_camera_textureview, container, false);
        mCameraTextureView = rootView.findViewById(R.id.fragment_camera_textureview_texture);
        mCameraTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraTextureView = null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        mCameraHelper.stopCamera();
        mCameraHelper.disconnect();
        super.onDestroy();
    }
}

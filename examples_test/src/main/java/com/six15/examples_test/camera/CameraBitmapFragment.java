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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.six15.examples.helpers.HudCameraHelper;
import com.six15.examples_test.R;
import com.six15.hudservice.CameraResolution;

import java.util.List;

public class CameraBitmapFragment extends Fragment {
    private static final String TAG = CameraBitmapFragment.class.getSimpleName();
    @Nullable
    private ImageView mCameraImageView;
    private HudCameraHelper mCameraHelper;
    private CameraResolution mVideoRes;
    @Nullable
    private Bitmap mCurrentBitmap = null;

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
                if (mCameraImageView != null) {
                    mCameraImageView.setImageBitmap(mCurrentBitmap);
                }
                return true;
            }
        });
        mCameraHelper.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCameraHelper.stopCamera();
        mCameraHelper.disconnect();
    }

    private void updateAspectRatioIfReady() {
        if (mCameraImageView == null || mVideoRes == null) {
            return;
        }
        ViewGroup.LayoutParams params = mCameraImageView.getLayoutParams();
        if (params instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams constraint_params = (ConstraintLayout.LayoutParams) params;
            int width = mVideoRes.getWidth();
            int height = mVideoRes.getHeight();
            constraint_params.dimensionRatio = width + ":" + height;
            mCameraImageView.setLayoutParams(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_camera_imageview, container, false);
        mCameraImageView = rootView.findViewById(R.id.fragment_camera_imageview_image);
        mCameraImageView.setImageBitmap(mCurrentBitmap);
        updateAspectRatioIfReady();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraImageView = null;
    }
}

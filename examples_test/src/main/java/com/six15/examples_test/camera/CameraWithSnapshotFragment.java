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

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import com.six15.examples.connection.HudCallbacks;
import com.six15.examples.connection.HudRetainedFragment;
import com.six15.examples.helpers.HudBitmapHelper;
import com.six15.examples.helpers.HudCameraHelper;
import com.six15.examples_test.R;
import com.six15.hudservice.CameraResolution;
import com.six15.hudservice.IHudService;
import com.six15.hudservice.ImageFrame;

import java.io.File;
import java.util.List;

public class CameraWithSnapshotFragment extends HudRetainedFragment {

    private static final String TAG = CameraWithSnapshotFragment.class.getSimpleName();
    private ImageView mCameraImageView;
    private HudCameraHelper mCameraHelper;
    private CameraResolution mSnapshotRes = null;
    private CameraResolution mVideoRes = null;
    private boolean mIsCapturingSnapshot = false;
    private IHudService mHmdService;
    private String mSavedSnapshotPath = null;
    private ImageView mShareImage;
    private boolean mDeviceConnected = false;
    private View mTakeSnapshotButton;
    private View mTakeZSLSnapshotButton;
    private Bitmap mCurrentVideoBitmap = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mCameraHelper = new HudCameraHelper(requireContext(), true, new HudCameraHelper.Callbacks() {
            @Nullable
            @Override
            protected CameraResolution getCameraResolution(@NonNull List<CameraResolution> resolutions) {
                //Resolutions are: [640x480, 320x240, 1280x720, 1920x1080, 2560x1440, 2592x1944]
                if (resolutions.size() == 0) {
                    throw new RuntimeException("We need at least 1 resolution to take a snapshot.");
                }

                if (mIsCapturingSnapshot) {
                    //Find the largest resolution to use for a snapshot.
                    //Alternivitly, you migth pick a resolution which has the same aspect ratio as video.
                    int largestArea = 0;
                    CameraResolution largestRes = null;
                    for (CameraResolution res : resolutions) {
                        int area = res.getWidth() * res.getHeight();
                        if (area > largestArea) {
                            largestRes = res;
                            largestArea = area;
                        }
                    }
                    mSnapshotRes = largestRes;
                    Log.d(TAG, "getCameraResolution() called with: resolutions = [" + resolutions + "]");
                    return mSnapshotRes;
                } else {
                    //Find the desired video resolution for streaming.

                    //480p has a high frame rate, and matches the aspect ratio of the sensor's largest (native) size.
                    //This make it good for previewing a snapshot.

                    //720p or 1080p are good choices for zero shutter lag, since they have reasonable quality and frame rate.
                    //They don't match the native sensor aspect ratio, but in ZSL that isn't a problem.
                    //1440p or 1944p are bad choices for video. They have low frame rates.
                    CameraResolution videoRes = null;
                    for (CameraResolution res : resolutions) {
                        if (res.getHeight() == 480) {
                            videoRes = res;
                            break;
                        }
                    }
                    //If there wasn't a match for some reason, just use the default.
                    if (videoRes == null) {
                        Log.w(TAG, "getCameraResolution: couldn't find a matching video resolution.");
                        videoRes = super.getCameraResolution(resolutions);
                    }
                    mVideoRes = videoRes;
                    updateAspectRatio();
                    return mVideoRes;
                }
            }

            @Override
            protected boolean onCameraBitmap(@Nullable Bitmap bitmap) {
                if (bitmap != null) {
                    //If this is a snapshot frame.
                    if (mSnapshotRes != null && bitmap.getWidth() == mSnapshotRes.getWidth() && bitmap.getHeight() == mSnapshotRes.getHeight()) {
                        //It's possible we get a snapshot frame when we don't want one. Skip that frame.
                        if (mIsCapturingSnapshot) {
                            Log.i(TAG, "onCameraBitmap: Snapshot!!");
                            //showCameraFrameOnHud(bitmap);
                            saveToFile(bitmap);
                            updateShareImage();
                            returnToLiveview();
                            //Set this bitmap as null, so the image view get cleared.
                        } else {
                            Log.i(TAG, "onCameraBitmap: Double snapshot");
                        }
                        //Return false, since the imageView is still holding a reference to the previous bitmap.
                        //That bitmap will be cleaned up by the garbage collector if/when another replaces it.
                        return false;
                    }
                }
                mCurrentVideoBitmap = bitmap;
                if (mCameraImageView != null) {
                    mCameraImageView.setImageBitmap(bitmap);
                }
                //Return true to allow the previous bitmap to be manually recycled (i.e. freed).
                //Manual memory cleanup greatly reduces peak memory usage when compared to relying on the garbage collector.
                return true;
            }
        });
        mCameraHelper.connect();
    }

    private void saveToFile(Bitmap bitmap) {
        //Save the bitmap to internal storage, i.e. context.getFilesDir()
        //The internal storage directory "external_files" is exposed by a content provider. See AndroidManifest.xml and provider_paths.xml
        mSavedSnapshotPath = HudBitmapHelper.saveBitmapAsJpeg(requireContext(), bitmap, 95, "external_files", "camera_snapshot");
    }

    private void shareSavedBitmap() {
        Uri imageUri = getSavedImageUri();
        if (imageUri == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(imageUri, "image/jpeg");
        intent.setClipData(ClipData.newUri(requireContext().getContentResolver(), "ST1 Snapshot", imageUri));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(intent, "Pick App to View Snapshot");
        requireContext().startActivity(chooser);
    }

    @Nullable
    private Uri getSavedImageUri() {
        if (mSavedSnapshotPath == null) {
            return null;
        }
        //Turn our file path into a Uri using our content provider as the authority.
        File file = new File(mSavedSnapshotPath);
        return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
    }

    private void showCameraFrameOnHud(Bitmap bitmap) {
        if (mHmdService == null) {
            return;
        }
        //Resize the bitmap to HUD size.
        Bitmap hudBitmap = HudBitmapHelper.calculateAdjustedBitmap(bitmap);
        try {
            mHmdService.sendImageToHud(new ImageFrame(hudBitmap));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateAspectRatio() {
        if (mCameraImageView == null || mVideoRes == null) {
            return;
        }
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mCameraImageView.getLayoutParams();
        params.dimensionRatio = mVideoRes.getWidth() + ":" + mVideoRes.getHeight();
        mCameraImageView.setLayoutParams(params);
    }

    private void updateShareImage() {
        if (mShareImage == null) {
            return;
        }
        Uri imageUri = getSavedImageUri();
        if (imageUri == null) {
            mShareImage.setImageResource(android.R.color.transparent);
        } else {
            //We re-use the same file name when saving the image, so the imageView will always see the same imageUri.
            // By setting the uri to null temporarily, we make the ImageView re-load imageUri from file.
            mShareImage.setImageURI(null);
            mShareImage.setImageURI(imageUri);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_camera_with_snapshot, null, false);
        mCameraImageView = rootView.findViewById(R.id.fragment_camera_with_snapshot_image);
        mTakeSnapshotButton = rootView.findViewById(R.id.fragment_camera_with_snapshot_capture_button);
        mTakeSnapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerCapture();
            }
        });
        mTakeZSLSnapshotButton = rootView.findViewById(R.id.fragment_camera_with_snapshot_capture_zsl_button);
        mTakeZSLSnapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerZeroShutterLagCapture();
            }
        });
        mShareImage = rootView.findViewById(R.id.fragment_camera_with_snapshot_share);
        mShareImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareSavedBitmap();
            }
        });
        updateAspectRatio();
        updateShareImage();
        updateTakeSnapshot();
        return rootView;
    }

    private void updateTakeSnapshot() {
        if (mTakeSnapshotButton == null || mTakeZSLSnapshotButton == null) {
            return;
        }
        mTakeZSLSnapshotButton.setEnabled(mDeviceConnected);
        mTakeSnapshotButton.setEnabled(mDeviceConnected);
    }


    @Override
    public void onDestroyView() {
        mTakeZSLSnapshotButton = null;
        mTakeSnapshotButton = null;
        mCameraImageView = null;
        mShareImage = null;
        super.onDestroyView();
    }

    private void triggerCapture() {
        mCameraHelper.stopCamera();
        mIsCapturingSnapshot = true;
        mCameraHelper.startCameraIfReady();
    }

    private void triggerZeroShutterLagCapture() {
        if (mCurrentVideoBitmap != null) {
            saveToFile(mCurrentVideoBitmap);
            updateShareImage();
        }
    }

    private void returnToLiveview() {
        mCameraHelper.stopCamera();
        mIsCapturingSnapshot = false;
        mCameraHelper.startCameraIfReady();
    }

    @Override
    public void onDestroy() {
        mCameraHelper.stopCamera();
        mCameraHelper.disconnect();
        super.onDestroy();
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
                mDeviceConnected = connected;
                updateTakeSnapshot();
            }
        };
    }
}

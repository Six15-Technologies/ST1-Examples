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

package com.six15.examples_test.intent_interface;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.six15.examples.HudIntentInterface;
import com.six15.examples.helpers.HudBitmapHelper;
import com.six15.examples_test.R;
import com.six15.hudservice.Constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IntentInterfaceExamplesActivity extends AppCompatActivity {

    private static final String TAG = IntentInterfaceExamplesActivity.class.getSimpleName();
    private boolean mIntentInterfaceEnabled = false;
    private ToggleButton mIntentInterfaceToggleButton;
    private View mExampleButton;
    private View mTimeButton;
    private View mPaddingButton;
    private View mGravityButton;
    private View mImageResourceButton;
    private View mSendLogoButton;

    private Handler mHandler;
    private boolean mRestoringInstanceState = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper());
        setContentView(R.layout.activity_intent_interface_examples);
        mIntentInterfaceToggleButton = findViewById(R.id.activity_intent_interface_examples_toggle_button);
        mIntentInterfaceToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mRestoringInstanceState || isChecked == mIntentInterfaceEnabled) {
                    return;
                }
                Context context = IntentInterfaceExamplesActivity.this;
                if (isChecked) {
                    Bundle args = showInitialImage(context);
                    HudIntentInterface.startIntentInterface(context, HudIntentInterface.ACTION_SEND_TEXT, args);
                } else {
                    mHandler.removeCallbacksAndMessages(null);
                    HudIntentInterface.stopIntentInterface(context);
                }
            }
        });
        mExampleButton = findViewById(R.id.activity_intent_interface_examples_example_button);
        mExampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacksAndMessages(null);
//                sendExample_Hardcoded();
                sendExample_UsingConstants();
            }
        });
        mTimeButton = findViewById(R.id.activity_intent_interface_examples_time_button);
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendTimeDate();
                        mHandler.postDelayed(this, 1000);
                    }
                });
                sendTimeDate();
            }
        });
        mPaddingButton = findViewById(R.id.activity_intent_interface_examples_padding_button);
        mPaddingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacksAndMessages(null);
                sendPadding();
            }
        });
        mGravityButton = findViewById(R.id.activity_intent_interface_examples_gravity_button);
        mGravityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacksAndMessages(null);
                sendGravity();
            }
        });
        mImageResourceButton = findViewById(R.id.activity_intent_interface_examples_action_send_res);
        mImageResourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacksAndMessages(null);
                sendImageResource();
            }
        });
        mSendLogoButton = findViewById(R.id.activity_intent_interface_examples_action_send_content);
        mSendLogoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacksAndMessages(null);
                sendSix15Logo();
            }
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HudIntentInterface.Response.ACTION_INTENT_SERVICE_STATE);

        registerReceiver(mReceiver, intentFilter);

        queryIntentInterfaceState();

    }

    private void sendImageResource() {
        Resources res = getResources();
        //Any of these work
        int resId = R.raw.test_image;
        //int resId = R.mipmap.ic_launcher;
        //int resId = R.drawable.ic_baseline_stop_circle_24;
        Uri imageUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(res.getResourcePackageName(resId))
                .appendPath(res.getResourceTypeName(resId))
                .appendPath(res.getResourceEntryName(resId))
                .build();

        //This does NOT work, since we require a "local uri"
        //See https://developer.android.com/reference/android/widget/ImageView#setImageURI(android.net.Uri)
        //Uri imageUri = Uri.parse("https://six15.engineering/_images/st1_product.png");

        //Many other types of URI's will work as well. It's best to stay away from file Uri's.

        HudIntentInterface.sendActionSend(this, imageUri);
    }

    private void sendSix15Logo() {
        //Allocate a bitmap. The size doesn't have to match the HUD, but it might as well.
        Bitmap bitmap = Bitmap.createBitmap(Constants.ST1_HUD_WIDTH, Constants.ST1_HUD_HEIGHT, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.BLACK);

        //Create a canvas which draws on the bitmap
        Canvas canvas = new Canvas(bitmap);

        //Draw a poor Six15 logo on the canvas
        Paint paint = new Paint();
        paint.setStrokeWidth(20);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        float w = Constants.ST1_HUD_WIDTH / 4.0f;
        float h = Constants.ST1_HUD_HEIGHT / 4.0f;
        paint.setColor(Color.WHITE);
        canvas.drawLine(w * 1, h * 1, w * 2, h * 2, paint);
        canvas.drawLine(w * 2, h * 2, w * 1, h * 3, paint);
        paint.setColor(getColor(R.color.six15_red));
        canvas.drawLine(w * 3, h * 1, w * 2, h * 2, paint);
        canvas.drawLine(w * 2, h * 2, w * 3, h * 3, paint);
        canvas.drawArc(w * 1.25f, h * 1.25f, w * 2.75f, h * 2.75f, -25, 50, false, paint);

        //Save the bitmap to internal storage, i.e. context.getFilesDir()
        //The internal storage directory "external_files" is exposed by a content provider. See AndroidManifest.xml and provider_paths.xml
        String path = HudBitmapHelper.saveBitmapAsJpeg(this, bitmap, 95, "external_files", "six15_logo");

        //Turn our file path into a Uri using our content provider as the authority.
        File file = new File(path);
        Uri imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

        HudIntentInterface.sendActionSend(this, imageUri);
    }

    private Bundle showInitialImage(Context context) {
        Bundle args = new Bundle();
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "0", "Example Initial Text");
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + "0", HudIntentInterface.colorToString(getColor(R.color.six15_dark_red)));

        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "1", " ");

        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "2", "Tap one of the buttons below");
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "3", "to show an example image.");

        return args;
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        mRestoringInstanceState = true;
        super.onRestoreInstanceState(savedInstanceState);
        mRestoringInstanceState = false;

    }

    private void sendExample_Hardcoded() {
        Intent intent = new Intent("com.six15.hudservice.ACTION_SEND_TEXT");

        intent.putExtra("text0", "Scan Location");
        intent.putExtra("bg_color0", "#454e83");
        intent.putExtra("weight0", "1");

        intent.putExtra("text1", new String[]{"Aisle", "Shelf", "Level"});
        intent.putExtra("bg_color1", "#20e5ff");
        intent.putExtra("color1", "BLACK");
        intent.putExtra("weight1", "1");

        intent.putExtra("text2", new String[]{"M58", "F10", "2"});
        intent.putExtra("weight2", "2");

        sendBroadcast(intent);
    }

    private void sendExample_UsingConstants() {
        Bundle args = new Bundle();

        int N;
        N = 0;
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + N, "Scan Location");
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + N, HudIntentInterface.colorToString(0xff454e83));//0xaarrggbb
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_WEIGHT_N + N, "1");
        N = 1;
        args.putStringArray(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + N, new String[]{"Aisle", "Shelf", "Level"});
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + N, HudIntentInterface.colorToString(0xff20e5ff));//0xaarrggbb
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_COLOR_N + N, HudIntentInterface.colorToString(Color.BLACK));
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_WEIGHT_N + N, "1");
        N = 2;
        args.putStringArray(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + N, new String[]{"M58", "F10", "2"});
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_WEIGHT_N + N, "2");

        HudIntentInterface.sendText(this, args);
    }

    private void sendTimeDate() {
        Date now = new Date();
        String currentDate = new SimpleDateFormat("M/dd", Locale.getDefault()).format(now);
        String currentTime = new SimpleDateFormat("h:mm:ss", Locale.getDefault()).format(now);

        Bundle args = new Bundle();

        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "0", "Intent Interface: Time");
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + "0", HudIntentInterface.colorToString(getColor(R.color.six15_dark_red)));

        args.putStringArray(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "1", new String[]{"Date", "Time"});
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + "1", HudIntentInterface.colorToString(0xff400000));//0xaarrggbb

        args.putStringArray(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "2", new String[]{currentDate, currentTime});
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_WEIGHT_N + "2", Float.toString(2.0f));

        HudIntentInterface.sendText(this, args);
    }

    private void sendPadding() {
        Bundle args = new Bundle();

        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "0", "Intent Interface: Padding");
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + "0", HudIntentInterface.colorToString(getColor(R.color.six15_dark_red)));

        addPaddingRow(args, 1, 0, 0);
        addPaddingRow(args, 2, 5, 5);
        addPaddingRow(args, 3, 10, 10);

        HudIntentInterface.sendText(this, args);
    }

    private void addPaddingRow(Bundle args, int row, int padding_h, int padding_v) {
        int grayLevel = 0x20 * row;
        args.putStringArray(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + row, new String[]{"h:" + padding_h, "v:" + padding_v});
        args.putInt(HudIntentInterface.EXTRA_SEND_TEXT_PADDING_HORIZONTAL_N + row, padding_h);
        args.putInt(HudIntentInterface.EXTRA_SEND_TEXT_PADDING_VERTICAL_N + row, padding_v);
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + row, HudIntentInterface.colorToString(Color.argb(0xff, grayLevel, grayLevel, grayLevel)));
    }

    private void sendGravity() {
        Bundle args = new Bundle();

        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + "0", "Intent Interface: Gravity");
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + "0", HudIntentInterface.colorToString(getColor(R.color.six15_dark_red)));

        addGravityRow(args, 1, "left");
        addGravityRow(args, 2, "center");
        addGravityRow(args, 3, "right");

        HudIntentInterface.sendText(this, args);
    }

    private void addGravityRow(Bundle args, int row, String gravity) {
        int grayLevel = 0x20 * row;
        int textR = (row - 1) % 3 == 0 ? 255 : 0;
        int textG = (row - 1) % 3 == 1 ? 255 : 0;
        int textB = (row - 1) % 3 == 2 ? 255 : 0;
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + row, gravity);
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_GRAVITY_N + row, gravity);
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + row, HudIntentInterface.colorToString(Color.argb(0xff, grayLevel, grayLevel, grayLevel)));
        args.putString(HudIntentInterface.EXTRA_SEND_TEXT_COLOR_N + row, HudIntentInterface.colorToString(Color.rgb(textR, textG, textB)));
    }

    private void queryIntentInterfaceState() {
        mIntentInterfaceEnabled = false;
        sendBroadcast(new Intent(HudIntentInterface.ACTION_INTENT_SERVICE_REQUEST_STATE));
        updateUi();
    }

    private void updateUi() {
        mIntentInterfaceToggleButton.setChecked(mIntentInterfaceEnabled);
        mExampleButton.setEnabled(mIntentInterfaceEnabled);
        mTimeButton.setEnabled(mIntentInterfaceEnabled);
        mPaddingButton.setEnabled(mIntentInterfaceEnabled);
        mGravityButton.setEnabled(mIntentInterfaceEnabled);
        mImageResourceButton.setEnabled(mIntentInterfaceEnabled);
        mSendLogoButton.setEnabled(mIntentInterfaceEnabled);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(HudIntentInterface.Response.ACTION_INTENT_SERVICE_STATE)) {
                mIntentInterfaceEnabled = intent.getBooleanExtra(HudIntentInterface.Response.EXTRA_INTENT_SERVICE_STATE_RUNNING, false);
                if (!mIntentInterfaceEnabled) {
                    mHandler.removeCallbacksAndMessages(null);
                }
                updateUi();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        unregisterReceiver(mReceiver);
    }
}

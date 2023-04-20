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

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HudIntentInterface {
    private static final String TAG = HudIntentInterface.class.getSimpleName();

    public static final String ACTION_START_INTENT_SERVICE = "com.six15.hudservice.ACTION_START_INTENT_SERVICE";

    public static final String ACTION_INTENT_SERVICE_REQUEST_STATE = "com.six15.hudservice.ACTION_INTENT_SERVICE_REQUEST_STATE";

    public static class Response {
        public static final String ACTION_INTENT_SERVICE_STATE = "com.six15.hudservice.ACTION_INTENT_SERVICE_STATE";
        public static final String EXTRA_INTENT_SERVICE_STATE_RUNNING = "running";
    }

    public static final String ACTION_SEND_TEXT = "com.six15.hudservice.ACTION_SEND_TEXT";
    public static final String EXTRA_SEND_TEXT_TEXT_N = "text";
    public static final String EXTRA_SEND_TEXT_COLOR_N = "color";
    public static final String EXTRA_SEND_TEXT_BG_COLOR_N = "bg_color";
    public static final String EXTRA_SEND_TEXT_PADDING_HORIZONTAL_N = "padding_horizontal";
    public static final String EXTRA_SEND_TEXT_PADDING_VERTICAL_N = "padding_vertical";
    public static final String EXTRA_SEND_TEXT_GRAVITY_N = "gravity";
    public static final String EXTRA_SEND_TEXT_WEIGHT_N = "weight";
    public static final String EXTRA_SEND_TEXT_MAX_LINES_N = "max_lines";
    public static final int SEND_TEXT_N_MAX = 4;//Max value of N in each parameter.
    public static final int SEND_TEXT_MAX_LINES_MAX = 4;//Max value of max_lines#.


    public static final String ACTION_CLEAR_DISPLAY = "com.six15.hudservice.ACTION_CLEAR_DISPLAY";

    public static final String ACTION_STOP_INTENT_SERVICE = "com.six15.hudservice.ACTION_STOP_INTENT_SERVICE";

    public static final String ACTION_SEND_SCREEN = "com.six15.hudservice.ACTION_SEND_SCREEN";
    public static final String EXTRA_SEND_SCREEN_LINES = "lines";


    public static void startIntentInterface(Context context) {
        startIntentInterface(context, null, null);
    }

    public static void startIntentInterface(Context context, @Nullable String initialAction, @Nullable Bundle args) {
        //Needs queries
        ResolveInfo resolveInfo = findFirstMatchingActivity(context);

        if (resolveInfo == null) {
            Log.w(TAG, "startIntentInterface: Could not find Intent Interface.");
            return;
        }

        String packageName = resolveInfo.activityInfo.packageName;
        String className = resolveInfo.activityInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        Intent explicitIntent = new Intent(HudIntentInterface.ACTION_START_INTENT_SERVICE);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        if (initialAction != null) {
            explicitIntent.setAction(initialAction);
        }
        if (args != null) {
            explicitIntent.putExtras(args);
        }
        explicitIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //Android 11 blocks apps (even foreground ones) from starting service's in apps which are not running.
        //The activity that is started will simply forward our request to the service while starting it, then close itself.
        //The activity is themed to be transparent.
        context.startActivity(explicitIntent);
    }

    @Nullable
    public static ResolveInfo findFirstMatchingActivity(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        //The following must be inside the manifest tag of your AndroidManifest.
        //    <queries>
        //        <intent>
        //            <action android:name="com.six15.hudservice.ACTION_START_INTENT_SERVICE" />
        //        </intent>
        //    </queries>

        Intent implicitIntent = new Intent(HudIntentInterface.ACTION_START_INTENT_SERVICE);
        @SuppressLint("QueryPermissionsNeeded")
        List<ResolveInfo> resolveInfo = pm.queryIntentActivities(implicitIntent, 0);

        if (resolveInfo == null) {
            return null;
        }
        for (String packageName : HudPackageNames.NAMES) {
            for (ResolveInfo info : resolveInfo) {
                if (info.activityInfo.packageName.equals(packageName)) {
                    return info;
                }
            }
        }
        if (resolveInfo.size() > 0) {
            return resolveInfo.get(0);
        }
        return null;
    }

    //Color.parseColor(String) can take String "#rrggbb" or "#aarrggbb" since it can check for 6 or 8 digits.
    //To go the other direction HudIntentInterface.colorToString(int) takes an int, but and can't differentiate values with or without alpha.
    //Therefore the int passed to colorToString MUST contain alpha.
    public static String colorToString(@ColorInt int color_with_alpha) {
        return String.format("#%08X", color_with_alpha);
    }

    public static void sendText(Context context, @NonNull Bundle extras) {
        Intent intent = new Intent(HudIntentInterface.ACTION_SEND_TEXT);
        intent.putExtras(extras);
        context.sendBroadcast(intent);
    }

    public static void sendScreen(Context context, ArrayList<String> lines_list) {
        Bundle extras = new Bundle();
        String[] lines_array = new String[lines_list.size()];
        lines_array = lines_list.toArray(lines_array);
        extras.putStringArray(EXTRA_SEND_SCREEN_LINES, lines_array);
        sendScreen(context, extras);
    }

    public static void sendScreen(Context context, String[] lines) {
        Bundle extras = new Bundle();
        extras.putStringArray(EXTRA_SEND_SCREEN_LINES, lines);
        sendScreen(context, extras);
    }

    public static void sendScreen(Context context, @NonNull Bundle extras) {
        Intent intent = new Intent(HudIntentInterface.ACTION_SEND_SCREEN);
        intent.putExtras(extras);
        context.sendBroadcast(intent);
    }

    public static void clearDisplay(Context context) {
        context.sendBroadcast(new Intent(HudIntentInterface.ACTION_CLEAR_DISPLAY));
    }

    public static void stopIntentInterface(Context context) {
        context.sendBroadcast(new Intent(HudIntentInterface.ACTION_STOP_INTENT_SERVICE));
    }

    public static void sendActionSend(Context context, Uri uri) {
        //Needs queries
        ResolveInfo resolveInfo = findFirstMatchingActivity(context);

        if (resolveInfo == null) {
            Log.w(TAG, "sendActionSend: Could not find Intent Interface.");
            return;
        }
        String packageName = resolveInfo.activityInfo.packageName;
        String className = resolveInfo.activityInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        Intent explicitIntent = new Intent(Intent.ACTION_SEND);

        // Set the component to be explicit
        explicitIntent.setComponent(component);
        explicitIntent.setType("image/*");//Android 13 requires this
        explicitIntent.putExtra(Intent.EXTRA_STREAM, uri);
        explicitIntent.setClipData(ClipData.newUri(context.getContentResolver(), null, uri));
        explicitIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(explicitIntent);
    }
}

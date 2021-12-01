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

import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.six15.hudservice.Constants;

public class HudDisplayHelper {
    private static final boolean DEBUG = false;

    private static final String TAG = HudDisplayHelper.class.getSimpleName();

    //In some situations it seems like released displays can still end up in this list.
    //This happens even though all virtual displays are released, and "dumpsys SurfaceFlinger" doesn't show them.
    //This function makes sure to grab the display with the highest displayId.
    @Nullable
    public static Display getHudPresentationDisplay(@NonNull DisplayManager displayManager) {
        Display[] presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        Display latestHudDisplay = null;
        int numDisplays = 0;
        for (Display display : presentationDisplays) {
            if (isHudDisplay(display)) {
                numDisplays++;
                if (latestHudDisplay == null || display.getDisplayId() > latestHudDisplay.getDisplayId()) {
                    if (latestHudDisplay != null) {
                        if (DEBUG)
                            Log.i(TAG, "getHudPresentationDisplay: Picking display:" + display.getDisplayId() + " over " + latestHudDisplay.getDisplayId());
                    }
                    latestHudDisplay = display;
                }
            }
        }
        if (DEBUG) {
            if (numDisplays > 1) {
                Log.i(TAG, "getHudPresentationDisplay: Found more than 1 display:" + numDisplays);
            }
            if (latestHudDisplay != null) {
                Log.i(TAG, "getHudPresentationDisplay: Picking Display:" + latestHudDisplay.getDisplayId());
            }
        }
        return latestHudDisplay;
    }

    public static boolean isHudDisplay(@NonNull Display display) {
        return Constants.VIRTUAL_DISPLAY_NAME.equals(display.getName());
    }
}

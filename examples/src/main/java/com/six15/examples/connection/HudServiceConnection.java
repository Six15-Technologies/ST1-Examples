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

package com.six15.examples.connection;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.six15.examples.HudPackageNames;
import com.six15.hudservice.Constants;
import com.six15.hudservice.IHudService;

import java.util.List;

public class HudServiceConnection {
    private static final String TAG = HudServiceConnection.class.getSimpleName();
    private final Context mContext;
    private final HudCallbacks mCallbacks;
    private final ServiceConnection mServiceConnection;
    IHudService mHudService;
    private boolean mNeedsToCallUnbind = false;

    public HudServiceConnection(@NonNull Context context, @NonNull HudCallbacks callbacks) {
        mContext = context;
        if (callbacks == null) {
            throw new IllegalArgumentException("HudCallbacks \"callbacks\" must not be null.");
        }
        mCallbacks = callbacks.getProxyCallbacks();

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mHudService = IHudService.Stub.asInterface(service);
                try {
                    if (mHudService.isInited()) {
                        mCallbacks.onServiceConnectionChanged(true, mHudService, null);
                        mHudService.registerCallback(mCallbacks);
                        return;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mHudService = null;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (mHudService != null) {
                    mHudService = null;
                    mCallbacks.onServiceConnectionChanged(true, null, null);
                }
            }

            @Override
            public void onNullBinding(ComponentName name) {
                try {
                    mContext.unbindService(mServiceConnection);
                } catch (IllegalArgumentException ignore) {
                }
                mNeedsToCallUnbind = false;
                Intent requestIntent = mContext.getPackageManager().getLaunchIntentForPackage(name.getPackageName());
                mCallbacks.onServiceConnectionChanged(false, null, requestIntent);
            }

            @Override
            public void onBindingDied(ComponentName name) {
                try {
                    mContext.unbindService(mServiceConnection);
                } catch (IllegalArgumentException ignore) {
                }
                mNeedsToCallUnbind = false;
                mCallbacks.onServiceConnectionChanged(false, null, null);
            }
        };
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            mContext.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void connectToService() {
        Intent i = createExplicitServiceFromImplicitIntent(mContext, new Intent(Constants.ACTION_HUD_SERVICE));
        if (i == null) {
            Log.e(TAG, "connectToService: No service found");
            String requestPermissionPackageName = null;
            for (String packageName : HudPackageNames.NAMES) {
                boolean installed = isPackageInstalled(packageName);
                if (installed) {
                    requestPermissionPackageName = packageName;
                    break;
                }
            }
            Intent requestIntent = null;
            if (requestPermissionPackageName != null) {
                requestIntent = mContext.getPackageManager().getLaunchIntentForPackage(requestPermissionPackageName);
            }
            mCallbacks.onServiceConnectionChanged(false, null, requestIntent);
            return;
        }
        Log.i(TAG, "connectToService: doing bind");
        mContext.bindService(i, mServiceConnection, Context.BIND_AUTO_CREATE);
        mNeedsToCallUnbind = true;

    }

    public void disconnectFromService() {
        if (mHudService != null) {
            try {
                mHudService.unregisterCallback(mCallbacks);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (mNeedsToCallUnbind) {
            mContext.unbindService(mServiceConnection);
            mNeedsToCallUnbind = false;
        }
    }

    public static Intent createExplicitServiceFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();

        //The following must be inside the manifest tag of your AndroidManifest. (assuming implicitIntent's action is "com.six15.hudservice.SERVICE")
        //    <queries>
        //        <intent>
        //            <action android:name="com.six15.hudservice.SERVICE" />
        //        </intent>
        //    </queries>
        @SuppressLint("QueryPermissionsNeeded")
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure we have an implementation.
        if (resolveInfo == null || resolveInfo.size() != 1) {
            if (resolveInfo != null) {
                Log.i(TAG, "createExplicitServiceFromImplicitIntent: Found multiple services. Num:" + resolveInfo.size());
            }
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    public static Intent createExplicitActivityFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();

        //The following must be inside the manifest tag of your AndroidManifest. (assuming implicitIntent's action is "com.six15.hudservice.ACTION_START_INTENT_SERVICE")
        //    <queries>
        //        <intent>
        //            <action android:name="com.six15.hudservice.ACTION_START_INTENT_SERVICE" />
        //        </intent>
        //    </queries>
        @SuppressLint("QueryPermissionsNeeded")
        List<ResolveInfo> resolveInfo = pm.queryIntentActivities(implicitIntent, 0);

        // Make sure we have an implementation.
        if (resolveInfo == null || resolveInfo.size() != 1) {
            if (resolveInfo != null) {
                Log.i(TAG, "createExplicitServiceFromImplicitIntent: Found multiple activities. Num:" + resolveInfo.size());
            }
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.activityInfo.packageName;
        String className = serviceInfo.activityInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }
}

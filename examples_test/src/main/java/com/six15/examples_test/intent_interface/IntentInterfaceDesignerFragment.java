package com.six15.examples_test.intent_interface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.six15.examples.HudIntentInterface;
import com.six15.examples_test.R;

public class IntentInterfaceDesignerFragment extends Fragment {

    private Button mSendButton;
    private EditText mTitle;
    private EditText mBody1;
    private EditText mBody2;
    private boolean mIntentInterfaceEnabled = false;
    private CompoundButton mIntentInterfaceToggleButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HudIntentInterface.Response.ACTION_INTENT_SERVICE_STATE);
        requireContext().registerReceiver(mReceiver, intentFilter);
        queryIntentInterfaceState();
    }

    private void queryIntentInterfaceState() {
        mIntentInterfaceEnabled = false;
        requireContext().sendBroadcast(new Intent(HudIntentInterface.ACTION_INTENT_SERVICE_REQUEST_STATE));
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
                updateUi();
            }
        }
    };

    private void updateUi() {
        if (mSendButton == null) {
            return;
        }
        mSendButton.setEnabled(mIntentInterfaceEnabled);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_intent_interface_designer, container, false);
        mIntentInterfaceToggleButton = rootView.findViewById(R.id.fragment_intent_interface_designer_toggle_button);
        mIntentInterfaceToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == mIntentInterfaceEnabled) {
                    return;
                }
                if (isChecked) {
                    HudIntentInterface.startIntentInterface(requireContext());
                } else {
                    HudIntentInterface.stopIntentInterface(requireContext());
                }
            }
        });

        mTitle = rootView.findViewById(R.id.fragment_intent_interface_designer_frame_title);
        mBody1 = rootView.findViewById(R.id.fragment_intent_interface_designer_frame_body1);
        mBody2 = rootView.findViewById(R.id.fragment_intent_interface_designer_frame_body2);

        mSendButton = rootView.findViewById(R.id.fragment_intent_interface_designer_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                String title = mTitle.getText().toString();
                String body1 = mBody1.getText().toString();
                String body2 = mBody2.getText().toString();
                if (TextUtils.isEmpty(title)) {
                    title = null;
                }
                if (TextUtils.isEmpty(body1)) {
                    body1 = null;
                }
                if (TextUtils.isEmpty(body2)) {
                    body2 = null;
                }
                String row;
                row = "0";
                args.putString(HudIntentInterface.EXTRA_SEND_TEXT_BG_COLOR_N + row, HudIntentInterface.colorToString(Color.BLUE));
                args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + row, title);
                args.putString(HudIntentInterface.EXTRA_SEND_TEXT_WEIGHT_N + row, "2");

                String weight;
                if (body1 == null || body2 == null) {
                    //One or both are empty.
                    weight = "6";
                } else {
                    weight = "3";
                }
                row = "1";
                args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + row, body1);
                args.putString(HudIntentInterface.EXTRA_SEND_TEXT_WEIGHT_N + row, weight);
                row = "2";
                args.putString(HudIntentInterface.EXTRA_SEND_TEXT_TEXT_N + row, body2);
                args.putString(HudIntentInterface.EXTRA_SEND_TEXT_WEIGHT_N + row, weight);
                HudIntentInterface.sendText(requireContext(), args);
            }
        });
        return rootView;
    }

    @Override
    public void onDestroyView() {
        mSendButton = null;
        mTitle = null;
        mBody1 = null;
        mBody2 = null;
        super.onDestroyView();
    }
}

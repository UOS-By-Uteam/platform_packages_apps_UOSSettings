/*
 * Copyright (C) 2016 The UOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.uos.settings.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.utils.SettingsDividerItemDecoration;
import com.android.settings.SettingsActivity;
import com.android.setupwizardlib.GlifPreferenceLayout;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

import com.android.settings.R;

public class NetworkTrafficWizard extends SettingsActivity {
    private static final String TAG = NetworkTrafficWizard.class.getSimpleName();

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, NetworkTrafficWizardFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return NetworkTrafficWizardFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    public static class NetworkTrafficWizardFragment extends SettingsPreferenceFragment 
            implements DialogInterface.OnClickListener {

        private static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";
        private static final String TURN_OFF_NETWORK_TRAFFIC_STATE = "turn_off_network_traffic_state";

        private Preference mNetTrafficState;
        private Preference mTurningOffTrafficState;

        @Override
        protected int getMetricsCategory() {
            return MetricsEvent.UOS_SETTINGS;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.uos_settings_networktraffic_setupwizard);

            mNetTrafficState = (Preference) findPreference(NETWORK_TRAFFIC_STATE);
            mTurningOffTrafficState = (Preference) findPreference(TURN_OFF_NETWORK_TRAFFIC_STATE);

            final int msgId;
            msgId = R.string.network_traffic_description;
            TextView message = (TextView) LayoutInflater.from(getActivity()).inflate(
                    R.layout.encryption_interstitial_header, null, false);
            message.setText(msgId);
            setHeaderView(message);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            layout.setDividerItemDecoration(new SettingsDividerItemDecoration(getContext()));

            layout.setIcon(getContext().getDrawable(R.drawable.network_traffic_icon));
            layout.setHeaderText(getActivity().getTitle());

            // Use the dividers in SetupWizardRecyclerLayout. Suppress the dividers in
            // PreferenceFragment.
            setDivider(null);
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
                Bundle savedInstanceState) {
            GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();
            if (key.equals(NETWORK_TRAFFIC_STATE)) {
                showDialog(1);
            } else {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 0);
                finishSettingup();
            }
            return true;
        }

        protected void finishSettingup() {
            finish();
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            final int titleId;
            final int messageId;
            titleId = R.string.show_network_traffic_unit;
            messageId = R.string.show_network_traffic_unit_detail;
            return new AlertDialog.Builder(getActivity())
                .setTitle(titleId)
                .setMessage(messageId)
                .setCancelable(true)
                .setPositiveButton(R.string.show_network_traffic_byte, this)
                .setNegativeButton(R.string.show_network_traffic_bit, this)
                .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 6);
                finishSettingup();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 2);
                finishSettingup();
            }
        }
    }
}

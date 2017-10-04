/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class TetheringHardwareAccelPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String TETHERING_HARDWARE_OFFLOAD = "tethering_hardware_offload";

    // We use the "disabled status" in code, but show the opposite text
    // on screen. So a value 0 indicates the tethering hardware accel is enabled.
    @VisibleForTesting
    static final int SETTING_VALUE_ON = 0;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 1;

    private SwitchPreference mPreference;

    public TetheringHardwareAccelPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return TETHERING_HARDWARE_OFFLOAD;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.TETHER_OFFLOAD_DISABLED,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int tetheringMode = Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.TETHER_OFFLOAD_DISABLED, 0 /* default */);
        mPreference.setChecked(tetheringMode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.TETHER_OFFLOAD_DISABLED, SETTING_VALUE_OFF);
        mPreference.setEnabled(false);
        mPreference.setChecked(false);
    }
}
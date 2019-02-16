package com.android.settings.deviceinfo.cerberus;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class CerberusInfoPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin{

    private final static String CERBERUS_INFO_KEY = "cerberus_info";
    private static final String CERBERUS_VERSION_PROP="ro.cerberus.version";

    private final Fragment mFragment;

    public CerberusInfoPreferenceController(Context context, Fragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            pref.setSummary(SystemProperties.get(CERBERUS_VERSION_PROP,
                    mContext.getResources().getString(R.string.device_info_default)));
        }
    }

    @Override
    public String getPreferenceKey() {
        return CERBERUS_INFO_KEY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        CerberusInfoDialogFragment.show(mFragment);
        return true;
    }
}

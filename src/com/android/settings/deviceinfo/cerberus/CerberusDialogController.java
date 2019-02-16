package com.android.settings.deviceinfo.cerberus;

import android.content.Context;
import android.os.SystemProperties;
import android.os.UserManager;
import com.android.settings.R;

public class CerberusDialogController {

    private static final String CERBERUS_VERSION_PROP = "ro.cerberus.version";
    private static final String CERBERUS_BUILD_DATE_PROP = "ro.cerberus.build_date";
    private static final String CERBERUS_BUILD_TYPE_PROP = "ro.cerberus.build_type";
    private static final String CERBERUS_DEVICE_PROP = "ro.product.model";
    private static final String CERBERUS_MAINTAINER_PROP = "ro.cerberus.maintainer";

    private static final int CERBERUS_VERSION_LABEL_ID = R.id.cerberus_version_label;
    private static final int CERBERUS_VERSION_VALUE_ID = R.id.cerberus_version_value;

    private static final int CERBERUS_BUILD_DATE_LABEL_ID = R.id.cerberus_build_date_label;
    private static final int CERBERUS_BUILD_DATE_VALUE_ID = R.id.cerberus_build_date_value;

    private static final int CERBERUS_BUILD_TYPE_LABEL_ID = R.id.cerberus_build_type_label;
    private static final int CERBERUS_BUILD_TYPE_VALUE_ID = R.id.cerberus_build_type_value;

    private static final int CERBERUS_DEVICE_LABEL_ID = R.id.cerberus_device_label;
    private static final int CERBERUS_DEVICE_VALUE_ID = R.id.cerberus_device_value;

    private static final int CERBERUS_MAINTAINER_LABEL_ID = R.id.cerberus_maintainer_label;
    private static final int CERBERUS_MAINTAINER_VALUE_ID = R.id.cerberus_maintainer_value;

    private final CerberusInfoDialogFragment mDialog;
    private final Context mContext;
    private final UserManager mUserManager;

    public CerberusDialogController(CerberusInfoDialogFragment dialog) {
        mDialog = dialog;
        mContext = dialog.getContext();
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
    }

    public void initialize() {

        mDialog.setText(CERBERUS_VERSION_VALUE_ID, SystemProperties.get(CERBERUS_VERSION_PROP,
                mContext.getResources().getString(R.string.device_info_default)));

        mDialog.setText(CERBERUS_BUILD_DATE_VALUE_ID, SystemProperties.get(CERBERUS_BUILD_DATE_PROP,
                mContext.getResources().getString(R.string.device_info_default)));

        mDialog.setText(CERBERUS_BUILD_TYPE_VALUE_ID, SystemProperties.get(CERBERUS_BUILD_TYPE_PROP,
                mContext.getResources().getString(R.string.device_info_default)));

        mDialog.setText(CERBERUS_DEVICE_VALUE_ID, SystemProperties.get(CERBERUS_DEVICE_PROP,
                mContext.getResources().getString(R.string.device_info_default)));

        mDialog.setText(CERBERUS_MAINTAINER_VALUE_ID, SystemProperties.get(CERBERUS_MAINTAINER_PROP,
                mContext.getResources().getString(R.string.device_info_default)));
    }
}

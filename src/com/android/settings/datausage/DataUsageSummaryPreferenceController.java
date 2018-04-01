/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.datausage;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.RecurrenceRule;

import com.android.internal.util.CollectionUtils;
import android.support.v7.widget.RecyclerView;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.net.DataUsageController;

import java.util.List;

/**
 * This is the controller for the top of the data usage screen that retrieves carrier data from the
 * new subscriptions framework API if available. The controller reads subscription information from
 * the framework and falls back to legacy usage data if none are available.
 */
public class DataUsageSummaryPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart {

    private static final String TAG = "DataUsageController";
    private static final String KEY = "status_header";
    private static final long PETA = 1000000000000000L;
    private static final float RELATIVE_SIZE_LARGE = 1.25f * 1.25f;  // (1/0.8)^2
    private static final float RELATIVE_SIZE_SMALL = 1.0f / RELATIVE_SIZE_LARGE;  // 0.8^2

    private final Activity mActivity;
    private final EntityHeaderController mEntityHeaderController;
    private final Lifecycle mLifecycle;
    private final DataUsageSummary mDataUsageSummary;
    private final DataUsageController mDataUsageController;
    private final DataUsageInfoController mDataInfoController;
    private final NetworkTemplate mDefaultTemplate;
    private final NetworkPolicyEditor mPolicyEditor;
    private final int mDataUsageTemplate;
    private final boolean mHasMobileData;
    private final SubscriptionManager mSubscriptionManager;

    /** Name of the carrier, or null if not available */
    private CharSequence mCarrierName;

    /** The number of registered plans, [0,N] */
    private int mDataplanCount;

    /** The time of the last update in milliseconds since the epoch, or -1 if unknown */
    private long mSnapshotTime;

    /**
     * The size of the first registered plan if one exists or the size of the warning if it is set.
     * -1 if no information is available.
     */
    private long mDataplanSize;
    /** The number of bytes used since the start of the cycle. */
    private long mDataplanUse;
    /** The starting time of the billing cycle in ms since the epoch */
    private long mCycleStart;
    /** The ending time of the billing cycle in ms since the epoch */
    private long mCycleEnd;

    private Intent mManageSubscriptionIntent;

    public DataUsageSummaryPreferenceController(Activity activity,
            Lifecycle lifecycle, DataUsageSummary dataUsageSummary) {
        super(activity, KEY);

        mActivity = activity;
        mEntityHeaderController = EntityHeaderController.newInstance(activity,
                dataUsageSummary, null);
        mLifecycle = lifecycle;
        mDataUsageSummary = dataUsageSummary;

        final int defaultSubId = DataUsageUtils.getDefaultSubscriptionId(activity);
        mDefaultTemplate = DataUsageUtils.getDefaultTemplate(activity, defaultSubId);
        NetworkPolicyManager policyManager = NetworkPolicyManager.from(activity);
        mPolicyEditor = new NetworkPolicyEditor(policyManager);

        mHasMobileData = DataUsageUtils.hasMobileData(activity)
                && defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        mDataUsageController = new DataUsageController(activity);
        mDataInfoController = new DataUsageInfoController();

        if (mHasMobileData) {
            mDataUsageTemplate = R.string.cell_data_template;
        } else if (DataUsageUtils.hasWifiRadio(activity)) {
            mDataUsageTemplate = R.string.wifi_data_template;
        } else {
            mDataUsageTemplate = R.string.ethernet_data_template;
        }

        mSubscriptionManager = (SubscriptionManager)
                mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    @VisibleForTesting
    DataUsageSummaryPreferenceController(
            DataUsageController dataUsageController,
            DataUsageInfoController dataInfoController,
            NetworkTemplate defaultTemplate,
            NetworkPolicyEditor policyEditor,
            int dataUsageTemplate,
            boolean hasMobileData,
            SubscriptionManager subscriptionManager,
            Activity activity,
            Lifecycle lifecycle,
            EntityHeaderController entityHeaderController,
            DataUsageSummary dataUsageSummary) {
        super(activity, KEY);
        mDataUsageController = dataUsageController;
        mDataInfoController = dataInfoController;
        mDefaultTemplate = defaultTemplate;
        mPolicyEditor = policyEditor;
        mDataUsageTemplate = dataUsageTemplate;
        mHasMobileData = hasMobileData;
        mSubscriptionManager = subscriptionManager;
        mActivity = activity;
        mLifecycle = lifecycle;
        mEntityHeaderController = entityHeaderController;
        mDataUsageSummary = dataUsageSummary;
    }

    @Override
    public void onStart() {
        RecyclerView view = mDataUsageSummary.getListView();
        mEntityHeaderController.setRecyclerView(view, mLifecycle);
        mEntityHeaderController.styleActionBar(mActivity);
    }

    @VisibleForTesting
    void setPlanValues(int dataPlanCount, long dataPlanSize, long dataPlanUse) {
        mDataplanCount = dataPlanCount;
        mDataplanSize = dataPlanSize;
        mDataplanUse = dataPlanUse;
    }

    @VisibleForTesting
    void setCarrierValues(String carrierName, long snapshotTime, long cycleEnd, Intent intent) {
        mCarrierName = carrierName;
        mSnapshotTime = snapshotTime;
        mCycleEnd = cycleEnd;
        mManageSubscriptionIntent = intent;
    }

    @Override
    public int getAvailabilityStatus() {
        return mSubscriptionManager.getDefaultDataSubscriptionInfo() != null
                ? AVAILABLE : DISABLED_UNSUPPORTED;
    }

    @Override
    public void updateState(Preference preference) {
        DataUsageSummaryPreference summaryPreference = (DataUsageSummaryPreference) preference;
        DataUsageController.DataUsageInfo info = mDataUsageController.getDataUsageInfo(
                mDefaultTemplate);

        mDataInfoController.updateDataLimit(info, mPolicyEditor.getPolicy(mDefaultTemplate));

        if (mSubscriptionManager != null) {
            refreshDataplanInfo(info);
        }

        if (info.warningLevel > 0 && info.limitLevel > 0) {
                summaryPreference.setLimitInfo(TextUtils.expandTemplate(
                        mContext.getText(R.string.cell_data_warning_and_limit),
                        Formatter.formatFileSize(mContext, info.warningLevel),
                        Formatter.formatFileSize(mContext, info.limitLevel)).toString());
        } else if (info.warningLevel > 0) {
                summaryPreference.setLimitInfo(TextUtils.expandTemplate(
                        mContext.getText(R.string.cell_data_warning),
                        Formatter.formatFileSize(mContext, info.warningLevel)).toString());
        } else if (info.limitLevel > 0) {
            summaryPreference.setLimitInfo(TextUtils.expandTemplate(
                    mContext.getText(R.string.cell_data_limit),
                    Formatter.formatFileSize(mContext, info.limitLevel)).toString());
        } else {
            summaryPreference.setLimitInfo(null);
        }

        summaryPreference.setUsageNumbers(mDataplanUse, mDataplanSize, mHasMobileData);

        if (mDataplanSize <= 0) {
            summaryPreference.setChartEnabled(false);
        } else {
            summaryPreference.setChartEnabled(true);
            summaryPreference.setLabels(Formatter.formatFileSize(mContext, 0 /* sizeBytes */),
                    Formatter.formatFileSize(mContext, mDataplanSize));
            summaryPreference.setProgress(mDataplanUse / (float) mDataplanSize);
        }
        summaryPreference.setUsageInfo(mCycleEnd, mSnapshotTime, mCarrierName,
                mDataplanCount, mManageSubscriptionIntent);
    }

    private String getLimitText(long limit, int textId) {
        if (limit <= 0) {
            return null;
        }
        return mContext.getString(textId, Formatter.formatFileSize(mContext, limit));
    }

    // TODO(b/70950124) add test for this method once the robolectric shadow run script is
    // completed (b/3526807)
    private void refreshDataplanInfo(DataUsageController.DataUsageInfo info) {
        // reset data before overwriting
        mCarrierName = null;
        mDataplanCount = 0;
        mDataplanSize = mDataInfoController.getSummaryLimit(info);
        mDataplanUse = info.usageLevel;
        mCycleStart = info.cycleStart;
        mCycleEnd = info.cycleEnd;
        mSnapshotTime = -1L;

        final int defaultSubId = SubscriptionManager.getDefaultSubscriptionId();
        final SubscriptionInfo subInfo = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (subInfo != null && mHasMobileData) {
            mCarrierName = subInfo.getCarrierName();
            List<SubscriptionPlan> plans = mSubscriptionManager.getSubscriptionPlans(defaultSubId);
            final SubscriptionPlan primaryPlan = getPrimaryPlan(mSubscriptionManager, defaultSubId);
            if (primaryPlan != null) {
                mDataplanCount = plans.size();
                mDataplanSize = primaryPlan.getDataLimitBytes();
                if (unlimited(mDataplanSize)) {
                    mDataplanSize = 0L;
                }
                mDataplanUse = primaryPlan.getDataUsageBytes();

                RecurrenceRule rule = primaryPlan.getCycleRule();
                if (rule != null && rule.start != null && rule.end != null) {
                    mCycleStart = rule.start.toEpochSecond() * 1000L;
                    mCycleEnd = rule.end.toEpochSecond() * 1000L;
                }
                mSnapshotTime = primaryPlan.getDataUsageTime();
            }
        }
        mManageSubscriptionIntent =
                mSubscriptionManager.createManageSubscriptionIntent(defaultSubId);
        Log.i(TAG, "Have " + mDataplanCount + " plans, dflt sub-id " + defaultSubId
                + ", intent " + mManageSubscriptionIntent);
    }

    public static SubscriptionPlan getPrimaryPlan(SubscriptionManager subManager, int primaryId) {
        List<SubscriptionPlan> plans = subManager.getSubscriptionPlans(primaryId);
        if (CollectionUtils.isEmpty(plans)) {
            return null;
        }
        // First plan in the list is the primary plan
        SubscriptionPlan plan = plans.get(0);
        return plan.getDataLimitBytes() > 0
                && saneSize(plan.getDataUsageBytes())
                && plan.getCycleRule() != null ? plan : null;
    }

    private static boolean saneSize(long value) {
        return value >= 0L && value < PETA;
    }

    public static boolean unlimited(long size) {
        return size == SubscriptionPlan.BYTES_UNLIMITED;
    }

    @VisibleForTesting
    private static CharSequence formatUsage(Context context, String template, long usageLevel) {
        final int FLAGS = Spannable.SPAN_INCLUSIVE_INCLUSIVE;

        final Formatter.BytesResult usedResult = Formatter.formatBytes(context.getResources(),
                usageLevel, Formatter.FLAG_CALCULATE_ROUNDED);
        final SpannableString enlargedValue = new SpannableString(usedResult.value);
        enlargedValue.setSpan(
                new RelativeSizeSpan(RELATIVE_SIZE_LARGE), 0, enlargedValue.length(), FLAGS);

        final SpannableString amountTemplate = new SpannableString(
                context.getString(com.android.internal.R.string.fileSizeSuffix)
                        .replace("%1$s", "^1").replace("%2$s", "^2"));
        final CharSequence formattedUsage = TextUtils.expandTemplate(amountTemplate,
                enlargedValue, usedResult.units);

        final SpannableString fullTemplate = new SpannableString(template);
        fullTemplate.setSpan(
                new RelativeSizeSpan(RELATIVE_SIZE_SMALL), 0, fullTemplate.length(), FLAGS);
        return TextUtils.expandTemplate(fullTemplate,
                BidiFormatter.getInstance().unicodeWrap(formattedUsage.toString()));
    }
}
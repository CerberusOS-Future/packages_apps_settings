package com.android.settings.deviceinfo.cerberus;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.android.internal.logging.nano.MetricsProto;

public class CerberusInfoDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "CerberusInfoDialog";
    private View mRootView;

    public static void show(Fragment host) {
        final FragmentManager manager = host.getChildFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final CerberusInfoDialogFragment dialog = new CerberusInfoDialogFragment();
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIALOG_FIRMWARE_VERSION;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.cerberus_title)
                .setPositiveButton(android.R.string.ok, null /* listener */);

        mRootView = LayoutInflater.from(getActivity()).inflate(
                R.layout.dialog_cerberus_info, null /* parent */);

        initializeControllers();

        return builder.setView(mRootView).create();
    }

    public void setText(int viewId, CharSequence text) {
        final TextView view = mRootView.findViewById(viewId);
        if (view != null) {
            view.setText(text);
        }
    }

    public void removeSettingFromScreen(int viewId) {
        final View view = mRootView.findViewById(viewId);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public void registerClickListener(int viewId, View.OnClickListener listener) {
        final View view = mRootView.findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }

    private void initializeControllers() {
        new CerberusDialogController(this).initialize();
    }
}

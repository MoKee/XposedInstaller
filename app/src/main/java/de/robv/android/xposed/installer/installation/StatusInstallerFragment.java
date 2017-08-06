package de.robv.android.xposed.installer.installation;

import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.installer.R;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.RootUtil;

public class StatusInstallerFragment extends Fragment {
    public static final File DISABLE_FILE = new File(XposedApp.BASE_DIR + "conf/disabled");
    public static final File ENABLE_FILE = new File(XposedApp.BASE_DIR + "conf/enabled");

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.status_installer, container, false);

        // Disable switch
        final SwitchCompat disableSwitch = (SwitchCompat) v.findViewById(R.id.disableSwitch);
        disableSwitch.setChecked(!DISABLE_FILE.exists() && ENABLE_FILE.exists());
        disableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    DISABLE_FILE.delete();
                    try {
                        ENABLE_FILE.createNewFile();
                    } catch (IOException e) {
                        Log.e(XposedApp.TAG, "Could not create " + ENABLE_FILE, e);
                    }
                    Snackbar.make(disableSwitch, R.string.xposed_on_next_reboot, Snackbar.LENGTH_LONG).show();
                } else {
                    try {
                        DISABLE_FILE.createNewFile();
                        ENABLE_FILE.delete();
                        Snackbar.make(disableSwitch, R.string.xposed_off_next_reboot, Snackbar.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Log.e(XposedApp.TAG, "Could not create " + DISABLE_FILE, e);
                    }
                }
            }
        });

        // Display warning dialog to new users
        if (!XposedApp.getPreferences().getBoolean("hide_install_warning", false)) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.install_warning_title)
                    .setMessage(R.string.install_warning)
                    .setNeutralButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            XposedApp.getPreferences().edit().putBoolean("hide_install_warning", true).apply();
                        }
                    })
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false)
                    .show();
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshInstallStatus();
    }

    private void refreshInstallStatus() {
        View v = getView();
        TextView txtInstallError = (TextView) v.findViewById(R.id.framework_install_errors);
        View txtInstallContainer = v.findViewById(R.id.status_container);
        ImageView txtInstallIcon = (ImageView) v.findViewById(R.id.status_icon);
        View disableWrapper = v.findViewById(R.id.disableView);

        // TODO This should probably compare the full version string, not just the number part.
        int active = XposedApp.getActiveXposedVersion();
        int installed = XposedApp.getInstalledXposedVersion();
        if (installed < 0) {
            txtInstallError.setText(R.string.framework_not_installed);
            txtInstallError.setTextColor(getResources().getColor(R.color.warning));
            txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.warning));
            txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_error));
            disableWrapper.setVisibility(View.GONE);
        } else if (installed != active) {
            txtInstallError.setText(getString(R.string.framework_not_active, XposedApp.getXposedProp().getVersion()));
            txtInstallError.setTextColor(getResources().getColor(R.color.amber_500));
            txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.amber_500));
            txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning));
        } else {
            txtInstallError.setText(getString(R.string.framework_active, XposedApp.getXposedProp().getVersion()));
            txtInstallError.setTextColor(getResources().getColor(R.color.darker_green));
            txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.darker_green));
            txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_circle));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_installer, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reboot:
            case R.id.soft_reboot:
                final RootUtil.RebootMode mode = RootUtil.RebootMode.fromId(item.getItemId());
                confirmReboot(mode.titleRes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RootUtil.reboot(mode, getActivity());
                    }
                });
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void confirmReboot(int contentTextId, DialogInterface.OnClickListener yesHandler) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.reboot_confirmation)
                .setPositiveButton(contentTextId, yesHandler)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}

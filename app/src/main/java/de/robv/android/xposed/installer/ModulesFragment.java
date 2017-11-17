package de.robv.android.xposed.installer;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.installer.repo.RepoDb;
import de.robv.android.xposed.installer.util.FrameworkUtil;
import de.robv.android.xposed.installer.util.ModuleUtil;
import de.robv.android.xposed.installer.util.ModuleUtil.InstalledModule;
import de.robv.android.xposed.installer.util.ModuleUtil.ModuleListener;
import de.robv.android.xposed.installer.util.NavUtil;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class ModulesFragment extends Fragment implements ModuleListener {

    public static final String SETTINGS_CATEGORY = "de.robv.android.xposed.category.MODULE_SETTINGS";

    public static final String PLAY_STORE_PACKAGE = "com.android.vending";
    public static final String PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=%s";
    private static final String KEY_HIDE_INSTALL_WARNING = "hide_install_warning";
    private static String PLAY_STORE_LABEL = null;
    private final ArrayList<InstalledModule> mModules = new ArrayList<>();

    private ModuleUtil mModuleUtil;
    private RecyclerView mModulesView;
    private View mEmptyView;
    private ModuleAdapter mAdapter = null;
    private PackageManager mPm = null;

    private Runnable reloadModules = new Runnable() {
        public void run() {
            mModules.clear();
            mModules.addAll(mModuleUtil.getModules().values());
            final Collator col = Collator.getInstance(Locale.getDefault());
            Collections.sort(mModules, (lhs, rhs) -> col.compare(lhs.getAppName(), rhs.getAppName()));
            mAdapter.notifyDataSetChanged();

            if (mModules.isEmpty()) {
                mModulesView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mModulesView.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mModuleUtil = ModuleUtil.getInstance();
        mPm = getActivity().getPackageManager();
        if (PLAY_STORE_LABEL == null) {
            try {
                ApplicationInfo ai = mPm.getApplicationInfo(PLAY_STORE_PACKAGE, 0);
                PLAY_STORE_LABEL = mPm.getApplicationLabel(ai).toString();
            } catch (NameNotFoundException ignored) {
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_modules, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new ModuleAdapter();
        mModulesView = (RecyclerView) view.findViewById(R.id.modules);
        mModulesView.setAdapter(mAdapter);

        mEmptyView = view.findViewById(R.id.empty);

        reloadModules.run();
        mModuleUtil.addListener(this);

        final SharedPreferences pref = XposedApp.getPreferences();
        if (!pref.getBoolean(KEY_HIDE_INSTALL_WARNING, false)) {
            final View disclaimerView = view.findViewById(R.id.disclaimer);
            disclaimerView.setVisibility(View.VISIBLE);
            view.findViewById(R.id.accept).setOnClickListener(v -> {
                if (pref.edit().putBoolean(KEY_HIDE_INSTALL_WARNING, true).commit()) {
                    disclaimerView.setVisibility(View.GONE);
                }
            });
        }

        final boolean isEnabled = FrameworkUtil.isEnabled();
        final boolean isLoaded = FrameworkUtil.isLoaded();

        if (isEnabled && !isLoaded) {
            view.findViewById(R.id.deactivated).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.exception)).setText(getString(
                    R.string.framework_not_active, XposedApp.getXposedProp().getVersion()));
        }

        final Switch masterView = (Switch) view.findViewById(R.id.master);
        masterView.setChecked(isEnabled);
        masterView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (FrameworkUtil.setEnable(isChecked)) {
                showSnackbarForReboot(isChecked
                        ? R.string.xposed_on_next_reboot
                        : R.string.xposed_off_next_reboot);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mModuleUtil.removeListener(this);
        mModulesView.setAdapter(null);
        mAdapter = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_installer, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reboot:
                confirmReboot();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, InstalledModule module) {
        getActivity().runOnUiThread(reloadModules);
    }

    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        getActivity().runOnUiThread(reloadModules);
    }

    private void onModuleItemClick(int position) {
        final InstalledModule module = mModules.get(position);
        final String packageName = module.packageName;
        final Intent launchIntent = getSettingsIntent(packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Snackbar.make(mModulesView, R.string.module_no_ui, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onModuleEnableChanged(int position, boolean isChecked) {
        final InstalledModule module = mModules.get(position);
        final String packageName = module.packageName;
        boolean changed = mModuleUtil.isModuleEnabled(packageName) ^ isChecked;
        if (changed) {
            mModuleUtil.setModuleEnabled(packageName, isChecked);
            if (mModuleUtil.updateModulesList()) {
                showSnackbarForReboot(R.string.reboot_needed);
            }
        }
    }

    private boolean onContextItemSelected(int position, MenuItem item) {
        final InstalledModule module = mModules.get(position);
        switch (item.getItemId()) {
            case R.id.menu_launch:
                startActivity(getSettingsIntent(module.packageName));
                return true;
            case R.id.menu_download_updates:
                startActivity(new Intent(getActivity(), DownloadDetailsActivity.class).setData(
                        Uri.fromParts("package", module.packageName, null)));
                return true;
            case R.id.menu_support:
                NavUtil.startURL(getActivity(),
                        Uri.parse(RepoDb.getModuleSupport(module.packageName)));
                return true;
            case R.id.menu_play_store:
                Intent i = new Intent(android.content.Intent.ACTION_VIEW);
                i.setData(Uri.parse(String.format(PLAY_STORE_LINK, module.packageName)));
                i.setPackage(PLAY_STORE_PACKAGE);
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    i.setPackage(null);
                    startActivity(i);
                }
                return true;
            case R.id.menu_app_info:
                startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", module.packageName, null)));
                return true;
            case R.id.menu_uninstall:
                startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE,
                        Uri.fromParts("package", module.packageName, null)));
                return true;
        }

        return false;
    }

    private Intent getSettingsIntent(String packageName) {
        // taken from
        // ApplicationPackageManager.getLaunchIntentForPackage(String)
        // first looks for an Xposed-specific category, falls back to
        // getLaunchIntentForPackage
        PackageManager pm = getActivity().getPackageManager();

        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(SETTINGS_CATEGORY);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, 0);

        if (ris == null || ris.size() <= 0) {
            return pm.getLaunchIntentForPackage(packageName);
        }

        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
        return intent;
    }

    private void showSnackbarForReboot(@StringRes int reason) {
        Snackbar.make(mModulesView, reason, Snackbar.LENGTH_LONG)
                .setAction(R.string.reboot, v -> confirmReboot())
                .show();
    }

    private void confirmReboot() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.reboot_confirmation)
                .setPositiveButton(R.string.reboot, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
                        powerManager.reboot(null);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ViewHolder> {

        final LayoutInflater inflater = LayoutInflater.from(getContext());

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(inflater.inflate(R.layout.list_item_module, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(mModules.get(position));
        }

        @Override
        public int getItemCount() {
            return mModules.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private final ImageView iconView;
            private final TextView titleView;
            private final TextView versionNameView;
            private final TextView descriptionView;
            private final TextView warningView;
            private final Switch enableView;
            private final PopupMenu popupMenu;

            ViewHolder(View itemView) {
                super(itemView);

                iconView = (ImageView) itemView.findViewById(R.id.icon);
                titleView = (TextView) itemView.findViewById(R.id.title);
                versionNameView = (TextView) itemView.findViewById(R.id.version_name);
                descriptionView = (TextView) itemView.findViewById(R.id.description);
                warningView = (TextView) itemView.findViewById(R.id.warning);
                enableView = (Switch) itemView.findViewById(R.id.enable);

                popupMenu = new PopupMenu(getContext(), itemView);
                popupMenu.inflate(R.menu.context_menu_modules);

                itemView.setOnClickListener(v ->
                        onModuleItemClick(getLayoutPosition()));

                itemView.setOnLongClickListener(v -> {
                    popupMenu.show();
                    return true;
                });

                enableView.setOnCheckedChangeListener((buttonView, isChecked) ->
                        onModuleEnableChanged(getLayoutPosition(), isChecked));

                popupMenu.setOnMenuItemClickListener(item ->
                        onContextItemSelected(getLayoutPosition(), item));
            }

            void bind(InstalledModule module) {
                final Menu menu = popupMenu.getMenu();

                iconView.setImageDrawable(module.getIcon());
                titleView.setText(module.getAppName());
                versionNameView.setText(module.versionName);

                if (TextUtils.isEmpty(module.getDescription())) {
                    descriptionView.setVisibility(View.GONE);
                } else {
                    descriptionView.setVisibility(View.VISIBLE);
                    descriptionView.setText(module.getDescription());
                }

                if (module.minVersion == 0) {
                    enableView.setEnabled(false);
                    warningView.setText(getString(R.string.no_min_version_specified));
                    warningView.setVisibility(View.VISIBLE);
                } else if (module.minVersion < ModuleUtil.MIN_MODULE_VERSION) {
                    enableView.setEnabled(false);
                    warningView.setText(getString(R.string.warning_min_version_too_low,
                            module.minVersion, ModuleUtil.MIN_MODULE_VERSION));
                    warningView.setVisibility(View.VISIBLE);
                } else if (module.isInstalledOnExternalStorage()) {
                    enableView.setEnabled(false);
                    warningView.setText(getString(R.string.warning_installed_on_external_storage));
                    warningView.setVisibility(View.VISIBLE);
                } else {
                    enableView.setEnabled(true);
                    warningView.setVisibility(View.GONE);
                }

                enableView.setChecked(mModuleUtil.isModuleEnabled(module.packageName));

                if (getSettingsIntent(module.packageName) == null) {
                    menu.removeItem(R.id.menu_launch);
                }

                try {
                    String support = RepoDb.getModuleSupport(module.packageName);
                    if (NavUtil.parseURL(support) == null) {
                        menu.removeItem(R.id.menu_support);
                    }
                } catch (RepoDb.RowNotFoundException e) {
                    menu.removeItem(R.id.menu_download_updates);
                    menu.removeItem(R.id.menu_support);
                }

                String installer = mPm.getInstallerPackageName(module.packageName);
                if (PLAY_STORE_LABEL != null && PLAY_STORE_PACKAGE.equals(installer)) {
                    menu.findItem(R.id.menu_play_store).setTitle(PLAY_STORE_LABEL);
                } else {
                    menu.removeItem(R.id.menu_play_store);
                }
            }

        }

    }

}

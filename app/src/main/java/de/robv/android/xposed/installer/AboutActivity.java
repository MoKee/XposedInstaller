package de.robv.android.xposed.installer;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.SILOpenFontLicense11;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;
import de.robv.android.xposed.installer.util.NavUtil;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.nav_item_about);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new AboutFragment())
                    .commit();
        }
    }

    public static class AboutFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.about);

            try {
                final String packageName = getActivity().getPackageName();
                final String version = getActivity().getPackageManager()
                        .getPackageInfo(packageName, 0).versionName;
                findPreference("version").setSummary(version);
            } catch (NameNotFoundException ignored) {
            }

            findPreference("framework").setSummary(XposedApp.getXposedProp().getVersion());

            findPreference("developers").setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    createDeveloperDialog();
                    return true;
                }
            });

            findPreference("licenses").setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    createLicenseDialog();
                    return true;
                }
            });

            if (TextUtils.isEmpty(getString(R.string.translator))) {
                getPreferenceScreen().removePreference(findPreference("translator"));
            }

            linkPreference("source", R.string.about_source);

            findPreference("modules").setSummary(getString(R.string.support_modules_description,
                    getString(R.string.module_support)));

            linkPreference("installer", R.string.about_support);
            linkPreference("faq", R.string.support_faq_url);
            linkPreference("donate", R.string.support_donate_url);
        }

        private void linkPreference(String key, final @StringRes int url) {
            findPreference(key).setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    NavUtil.startURL(getActivity(), getString(url));
                    return true;
                }
            });
        }

        private void createDeveloperDialog() {
            final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.about_developers_label)
                    .setMessage(R.string.about_developers)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();

            final TextView message = (TextView) dialog.findViewById(android.R.id.message);
            if (message != null) {
                message.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        @SuppressWarnings("SpellCheckingInspection")
        private void createLicenseDialog() {
            Notices notices = new Notices();

            notices.addNotice(new Notice(
                    "StickyListHeaders",
                    "https://github.com/emilsjolander/StickyListHeaders",
                    "Emil Sjölander",
                    new ApacheSoftwareLicense20()));

            notices.addNotice(new Notice(
                    "PreferenceFragment-Compat",
                    "https://github.com/Machinarius/PreferenceFragment-Compat",
                    "machinarius",
                    new ApacheSoftwareLicense20()));

            notices.addNotice(new Notice(
                    "libsuperuser",
                    "https://github.com/Chainfire/libsuperuser",
                    "Copyright (C) 2012-2015 Jorrit \"Chainfire\" Jongma",
                    new ApacheSoftwareLicense20()));

            notices.addNotice(new Notice(
                    "picasso",
                    "https://github.com/square/picasso",
                    "Copyright 2013 Square, Inc.",
                    new ApacheSoftwareLicense20()));

            notices.addNotice(new Notice(
                    "materialdesignicons",
                    "http://materialdesignicons.com",
                    "Copyright (c) 2014, Austin Andrews",
                    new SILOpenFontLicense11()));

            new LicensesDialog.Builder(getActivity())
                    .setNotices(notices)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show();
        }

    }
}

/*
 * Copyright (C) 2013 rovo89, Tungstwenty
 * Copyright (C) 2017 The MoKee Open Source Project
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
import android.support.v7.preference.PreferenceGroup;
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

            if (XposedApp.mkVerified) {
                ((PreferenceGroup)findPreference("support")).removePreference(findPreference("download"));
            } else {
                linkPreference("download", R.string.support_download_url);
            }

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

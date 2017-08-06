package de.robv.android.xposed.installer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.installer.util.RepoLoader;

public class SettingsActivity extends AppCompatActivity {

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
            ab.setTitle(R.string.nav_item_settings);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment()).commit();
        }

    }

    public static class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private static final File mDisableResourcesFlag = new File(XposedApp.BASE_DIR + "conf/disable_resources");

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.prefs);

            findPreference("release_type_global").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    RepoLoader.getInstance().setReleaseTypeGlobal((String) newValue);
                    return true;
                }
            });

            CheckBoxPreference prefDisableResources = (CheckBoxPreference) findPreference("disable_resources");
            prefDisableResources.setChecked(mDisableResourcesFlag.exists());
            prefDisableResources.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            mDisableResourcesFlag.createNewFile();
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //noinspection ResultOfMethodCallIgnored
                        mDisableResourcesFlag.delete();
                    }
                    return (enabled == mDisableResourcesFlag.exists());
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("theme")) getActivity().recreate();
        }
    }
}

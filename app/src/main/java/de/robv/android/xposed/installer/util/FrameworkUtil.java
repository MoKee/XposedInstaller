package de.robv.android.xposed.installer.util;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.installer.XposedApp;

public class FrameworkUtil {

    private static final File DISABLE_FILE = new File(XposedApp.BASE_DIR + "conf/disabled");

    // Used by MoKee integration, since we want to disable Xposed by default
    private static final File ENABLE_FILE = new File(XposedApp.BASE_DIR + "conf/enabled");

    public static boolean isLoaded() {
        return XposedApp.getActiveXposedVersion() == XposedApp.getInstalledXposedVersion();
    }

    public static boolean isEnabled() {
        return !DISABLE_FILE.exists() && ENABLE_FILE.exists();
    }

    public static boolean setEnable(boolean enable) {
        if (enable) {
            try {
                //noinspection ResultOfMethodCallIgnored
                DISABLE_FILE.delete();
            } catch (Exception ignored) {
            }

            try {
                return ENABLE_FILE.createNewFile();
            } catch (IOException e) {
                Log.e(XposedApp.TAG, "Could not create " + ENABLE_FILE, e);
                return false;
            }
        } else {
            try {
                //noinspection ResultOfMethodCallIgnored
                DISABLE_FILE.createNewFile();
            } catch (Exception ignored) {
            }

            return ENABLE_FILE.delete();
        }
    }

}

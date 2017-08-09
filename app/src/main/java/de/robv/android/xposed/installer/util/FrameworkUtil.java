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
        if (XposedApp.mkVerified) {
            return !DISABLE_FILE.exists() && ENABLE_FILE.exists();
        } else {
            return !DISABLE_FILE.exists();
        }
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

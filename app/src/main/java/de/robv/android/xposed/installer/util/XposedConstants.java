package de.robv.android.xposed.installer.util;

import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;

@SuppressWarnings("WeakerAccess")
public class XposedConstants {

    public static final boolean MOKEE_INTEGRATION = !TextUtils.isEmpty(SystemProperties.get("ro.mk.version"));

    private static final String BASE_DIR_LEGACY = "/data/data/de.robv.android.xposed.installer/";

    public static final String BASE_DIR = Build.VERSION.SDK_INT >= 24
            ? "/data/user_de/0/de.robv.android.xposed.installer/" : BASE_DIR_LEGACY;

    public static final String BIN_DIR = BASE_DIR + "/bin";
    public static final String LOG_DIR = BASE_DIR + "/log";
    public static final String CONF_DIR = BASE_DIR + "/conf";

    public static final String CONF_DISABLED = CONF_DIR + "/disabled";
    public static final String CONF_MOKEE_INTEGRATION_ENABLED = CONF_DIR + "/enabled";

    public static final String CONF_DISABLE_RESOURCES = CONF_DIR + "/disable_resources";

    public static final String CONF_MODULES = CONF_DIR + "/modules.list";
    public static final String CONF_ENABLED_MODULES = CONF_DIR + "/enabled_modules.list";

}

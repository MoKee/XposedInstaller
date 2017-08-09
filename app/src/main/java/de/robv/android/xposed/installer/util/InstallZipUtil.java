package de.robv.android.xposed.installer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class InstallZipUtil {

    private InstallZipUtil() {
    }

    public static XposedProp parseXposedProp(InputStream is) throws IOException {
        XposedProp prop = new XposedProp();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String key = parts[0].trim();
            if (key.charAt(0) == '#') {
                continue;
            }

            String value = parts[1].trim();

            switch (key) {
                case "version":
                    prop.mVersion = value;
                    prop.mVersionInt = ModuleUtil.extractIntPart(value);
                    break;
                case "arch":
                    prop.mArch = value;
                    break;
                case "minsdk":
                    prop.mMinSdk = Integer.parseInt(value);
                    break;
                case "maxsdk":
                    prop.mMaxSdk = Integer.parseInt(value);
                    break;
            }
        }
        reader.close();
        return prop.isComplete() ? prop : null;
    }

    public static class XposedProp {

        private String mVersion = null;
        private int mVersionInt = 0;
        private String mArch = null;
        private int mMinSdk = 0;
        private int mMaxSdk = 0;

        private boolean isComplete() {
            return mVersion != null
                    && mVersionInt > 0
                    && mArch != null
                    && mMinSdk > 0
                    && mMaxSdk > 0;
        }

        public String getVersion() {
            return mVersion;
        }

        public int getVersionInt() {
            return mVersionInt;
        }

    }
}

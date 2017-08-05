package de.robv.android.xposed.installer.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import java.io.File;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.repo.RepoDbDefinitions.InstalledModulesColumns;
import de.robv.android.xposed.installer.repo.RepoDbDefinitions.InstalledModulesUpdatesColumns;
import de.robv.android.xposed.installer.repo.RepoDbDefinitions.ModuleVersionsColumns;
import de.robv.android.xposed.installer.repo.RepoDbDefinitions.ModulesColumns;
import de.robv.android.xposed.installer.repo.RepoDbDefinitions.MoreInfoColumns;
import de.robv.android.xposed.installer.repo.RepoDbDefinitions.RepositoriesColumns;
import de.robv.android.xposed.installer.util.ModuleUtil.InstalledModule;

public final class RepoDb extends SQLiteOpenHelper {
    private static SQLiteDatabase sDb;

    private RepoDb(Context context) {
        super(context, getDbPath(context), null, RepoDbDefinitions.DATABASE_VERSION);
    }

    private static String getDbPath(Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new File(context.getNoBackupFilesDir(), RepoDbDefinitions.DATABASE_NAME).getPath();
        } else {
            return RepoDbDefinitions.DATABASE_NAME;
        }
    }

    static {
        RepoDb instance = new RepoDb(XposedApp.getInstance());
        sDb = instance.getWritableDatabase();
        sDb.execSQL("PRAGMA foreign_keys=ON");
        instance.createTempTables(sDb);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RepoDbDefinitions.SQL_CREATE_TABLE_REPOSITORIES);
        db.execSQL(RepoDbDefinitions.SQL_CREATE_TABLE_MODULES);
        db.execSQL(RepoDbDefinitions.SQL_CREATE_TABLE_MODULE_VERSIONS);
        db.execSQL(RepoDbDefinitions.SQL_CREATE_INDEX_MODULE_VERSIONS_MODULE_ID);
        db.execSQL(RepoDbDefinitions.SQL_CREATE_TABLE_MORE_INFO);
    }

    private void createTempTables(SQLiteDatabase db) {
        db.execSQL(RepoDbDefinitions.SQL_CREATE_TEMP_TABLE_INSTALLED_MODULES);
        db.execSQL(RepoDbDefinitions.SQL_CREATE_TEMP_VIEW_INSTALLED_MODULES_UPDATES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This is only a cache, so simply drop & recreate the tables
        db.execSQL("DROP TABLE IF EXISTS " + RepositoriesColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ModulesColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ModuleVersionsColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MoreInfoColumns.TABLE_NAME);

        db.execSQL("DROP TABLE IF EXISTS " + InstalledModulesColumns.TABLE_NAME);
        db.execSQL("DROP VIEW IF EXISTS " + InstalledModulesUpdatesColumns.VIEW_NAME);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static void beginTransation() {
        sDb.beginTransaction();
    }

    public static void setTransactionSuccessful() {
        sDb.setTransactionSuccessful();
    }

    public static void endTransation() {
        sDb.endTransaction();
    }

    private static String getString(String table, String searchColumn, String searchValue, String resultColumn) {
        String[] projection = new String[]{resultColumn};
        String where = searchColumn + " = ?";
        String[] whereArgs = new String[]{searchValue};
        Cursor c = sDb.query(table, projection, where, whereArgs, null, null, null, "1");
        if (c.moveToFirst()) {
            String result = c.getString(c.getColumnIndexOrThrow(resultColumn));
            c.close();
            return result;
        } else {
            c.close();
            throw new RowNotFoundException("Could not find " + table + "." + searchColumn + " with value '" + searchValue + "'");
        }
    }

    public static String getModuleSupport(String packageName) {
        return getString(ModulesColumns.TABLE_NAME, ModulesColumns.PKGNAME, packageName, ModulesColumns.SUPPORT);
    }

    public static long insertInstalledModule(InstalledModule installed) {
        ContentValues values = new ContentValues();
        values.put(InstalledModulesColumns.PKGNAME, installed.packageName);
        values.put(InstalledModulesColumns.VERSION_CODE, installed.versionCode);
        values.put(InstalledModulesColumns.VERSION_NAME, installed.versionName);
        return sDb.insertOrThrow(InstalledModulesColumns.TABLE_NAME, null, values);
    }

    public static void deleteInstalledModule(String packageName) {
        sDb.delete(InstalledModulesColumns.TABLE_NAME, InstalledModulesColumns.PKGNAME + " = ?", new String[]{packageName});
    }

    public static void deleteAllInstalledModules() {
        sDb.delete(InstalledModulesColumns.TABLE_NAME, null, null);
    }

    public static class RowNotFoundException extends RuntimeException {
        private static final long serialVersionUID = -396324186622439535L;

        public RowNotFoundException(String reason) {
            super(reason);
        }
    }
}

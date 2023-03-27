package io.github.doodle;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.net.Uri;
import android.content.Context;

/**
 * Use this to get the ApplicationContext.
 */
public final class DoodleContentProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Context appContext = getContext().getApplicationContext();
        Utils.appContext = appContext;
        Utils.registerActivityLifecycle(appContext);
        return false;
    }

    public static Context getAppContext() {
        return Utils.appContext;
    }

    @Override
    public android.database.Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}

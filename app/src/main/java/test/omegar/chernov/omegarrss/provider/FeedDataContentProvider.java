package test.omegar.chernov.omegarrss.provider;


import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import test.omegar.chernov.omegarrss.BuildConfig;
import test.omegar.chernov.omegarrss.Constants;
import test.omegar.chernov.omegarrss.provider.FeedData.EntryColumns;
import test.omegar.chernov.omegarrss.provider.FeedData.TaskColumns;


public class FeedDataContentProvider extends ContentProvider {

    public static final int URI_ENTRIES = 13;
    public static final int URI_ENTRY = 14;
    public static final int URI_TASKS = 19;
    public static final int URI_TASK = 20;

    public static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entries", URI_ENTRIES);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entries/#", URI_ENTRY);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "tasks", URI_TASKS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "tasks/#", URI_TASK);
    }

    private DatabaseHelper mDatabaseHelper;

    @Override
    public String getType(Uri uri) {
        int matchCode = URI_MATCHER.match(uri);

        switch (matchCode) {
            case URI_ENTRIES:
                return "vnd.android.cursor.dir/vnd.omegarss.entry";
            case URI_ENTRY:
                return "vnd.android.cursor.item/vnd.omegarss.entry";
            case URI_TASKS:
                return "vnd.android.cursor.dir/vnd.omegarss.task";
            case URI_TASK:
                return "vnd.android.cursor.item/vnd.omegarss.task";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // This is a debug code to allow to visualize the task with the ContentProviderHelper app
        if (uri != null && BuildConfig.DEBUG && FeedData.CONTENT_AUTHORITY.equals(uri.toString())) {
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            queryBuilder.setTables(TaskColumns.TABLE_NAME);
            SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
            return queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
        }

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int matchCode = URI_MATCHER.match(uri);

        switch (matchCode) {
            case URI_ENTRIES: {
                queryBuilder.setTables(EntryColumns.TABLE_NAME);
                break;
            }
            case URI_ENTRY: {
                queryBuilder.setTables(EntryColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_TASKS: {
                queryBuilder.setTables(TaskColumns.TABLE_NAME);
                break;
            }
            case URI_TASK: {
                queryBuilder.setTables(TaskColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            default:
                throw new IllegalArgumentException("Illegal query. Match code=" + matchCode + "; uri=" + uri);
        }

        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();

        Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long newId;

        int matchCode = URI_MATCHER.match(uri);

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        switch (matchCode) {
            case URI_ENTRIES: {
                newId = database.insert(EntryColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_TASKS: {
                newId = database.insert(TaskColumns.TABLE_NAME, null, values);
                break;
            }
            default:
                throw new IllegalArgumentException("Illegal insert. Match code=" + matchCode + "; uri=" + uri);
        }

        if (newId > -1) {
            notifyChangeOnAllUris(matchCode, uri);
            return ContentUris.withAppendedId(uri, newId);
        } else { // This can happen when an insert failed with "ON CONFLICT IGNORE", this is not an error
            return uri;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (uri == null || values == null) {
            throw new IllegalArgumentException("Illegal update. Uri=" + uri + "; values=" + values);
        }

        int matchCode = URI_MATCHER.match(uri);

        String table;

        StringBuilder where = new StringBuilder();

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        switch (matchCode) {
            case URI_ENTRIES: {
                table = EntryColumns.TABLE_NAME;
                break;
            }
            case URI_ENTRY: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_TASKS: {
                table = TaskColumns.TABLE_NAME;
                break;
            }
            case URI_TASK: {
                table = TaskColumns.TABLE_NAME;
                where.append(TaskColumns._ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            default:
                throw new IllegalArgumentException("Illegal update. Match code=" + matchCode + "; uri=" + uri);
        }

        if (!TextUtils.isEmpty(selection)) {
            if (where.length() > 0) {
                where.append(Constants.DB_AND).append(selection);
            } else {
                where.append(selection);
            }
        }

        int count = database.update(table, values, where.toString(), selectionArgs);

        if (count > 0) {
            notifyChangeOnAllUris(matchCode, uri);
        }

        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int matchCode = URI_MATCHER.match(uri);

        String table;

        StringBuilder where = new StringBuilder();

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        switch (matchCode) {
            case URI_ENTRIES: {
                table = EntryColumns.TABLE_NAME;

                // Also remove all tasks
                new Thread() {
                    @Override
                    public void run() {
                        delete(TaskColumns.CONTENT_URI, null, null);
                    }
                }.start();
                break;
            }
            case URI_ENTRY: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_TASKS: {
                table = TaskColumns.TABLE_NAME;
                break;
            }
            case URI_TASK: {
                table = TaskColumns.TABLE_NAME;
                where.append(TaskColumns._ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            default:
                throw new IllegalArgumentException("Illegal delete. Match code=" + matchCode + "; uri=" + uri);
        }

        if (!TextUtils.isEmpty(selection)) {
            if (where.length() > 0) {
                where.append(Constants.DB_AND);
            }
            where.append(selection);
        }

        int count = database.delete(table, where.toString(), selectionArgs);

        if (count > 0) {
            notifyChangeOnAllUris(matchCode, uri);
        }
        return count;
    }

    private void notifyChangeOnAllUris(int matchCode, Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null);
    }
}

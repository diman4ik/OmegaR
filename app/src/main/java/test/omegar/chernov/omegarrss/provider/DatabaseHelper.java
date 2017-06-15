package test.omegar.chernov.omegarrss.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "rssfeed.db";
    private static final int DATABASE_VERSION = 1;

    public static final String LOG_TAG = DatabaseHelper.class.getSimpleName();


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(createTable(FeedData.EntryColumns.TABLE_NAME, FeedData.EntryColumns.COLUMNS));
        database.execSQL(createTable(FeedData.TaskColumns.TABLE_NAME, FeedData.TaskColumns.COLUMNS));
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int ver1, int ver2) {

    }

    private String createTable(String tableName, String[][] columns) {
        if (tableName == null || columns == null || columns.length == 0) {
            throw new IllegalArgumentException("Invalid parameters for creating table " + tableName);
        } else {
            StringBuilder stringBuilder = new StringBuilder("CREATE TABLE ");

            stringBuilder.append(tableName);
            stringBuilder.append(" (");
            for (int n = 0, i = columns.length; n < i; n++) {
                if (n > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(columns[n][0]).append(' ').append(columns[n][1]);
            }
            return stringBuilder.append(");").toString();
        }
    }
}

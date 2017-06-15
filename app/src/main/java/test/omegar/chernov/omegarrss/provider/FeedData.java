package test.omegar.chernov.omegarrss.provider;

import android.net.Uri;
import android.provider.BaseColumns;


public class FeedData {
    public static final String CONTENT = "content://";
    public static final String AUTHORITY = "test.omegar.chernov.omegarrss.provider.FeedData";
    public static final String CONTENT_AUTHORITY = CONTENT + AUTHORITY;
    static final String TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY AUTOINCREMENT";
    static final String TYPE_EXTERNAL_ID = "INTEGER(7)";
    static final String TYPE_TEXT = "TEXT";
    static final String TYPE_TEXT_UNIQUE = "TEXT UNIQUE";
    static final String TYPE_DATE_TIME = "DATETIME";
    static final String TYPE_INT = "INT";
    static final String TYPE_BOOLEAN = "INTEGER(1)";


    public static class EntryColumns implements BaseColumns {
        public static final String TABLE_NAME = "entries";

        public static final String TITLE = "title";
        public static final String ABSTRACT = "abstract";
        public static final String DATE = "date";
        public static final String FETCH_DATE = "fetch_date";
        public static final String LINK = "link";
        public static final String ENCLOSURE = "enclosure";
        public static final String GUID = "guid";
        public static final String AUTHOR = "author";
        public static final String IMAGE_URL = "image_url";

        public static final String[][] COLUMNS = new String[][]{{_ID, TYPE_PRIMARY_KEY}, {TITLE, TYPE_TEXT},
                {ABSTRACT, TYPE_TEXT}, {DATE, TYPE_DATE_TIME}, {FETCH_DATE, TYPE_DATE_TIME}, {LINK, TYPE_TEXT},
                {ENCLOSURE, TYPE_TEXT}, {GUID, TYPE_TEXT}, {AUTHOR, TYPE_TEXT}, {IMAGE_URL, TYPE_TEXT}};

        public static Uri CONTENT_URI(String entryId) {
            return Uri.parse(CONTENT_AUTHORITY + "/entries/" + entryId);
        }

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/entries");

        public static Uri CONTENT_URI(long entryId) {
            return Uri.parse(CONTENT_AUTHORITY + "/entries/" + entryId);
        }
    }

    public static class TaskColumns implements BaseColumns {
        public static final String TABLE_NAME = "tasks";

        public static final String ENTRY_ID = "entryid";
        public static final String IMG_URL_TO_DL = "imgurl_to_dl";
        public static final String NUMBER_ATTEMPT = "number_attempt";

        public static Uri CONTENT_URI(String taskId) {
            return Uri.parse(CONTENT_AUTHORITY + "/tasks/" + taskId);
        }

        public static Uri CONTENT_URI(long taskId) {
            return Uri.parse(CONTENT_AUTHORITY + "/tasks/" + taskId);
        }

        public static final String[][] COLUMNS = new String[][]{{_ID, TYPE_PRIMARY_KEY}, {ENTRY_ID, TYPE_EXTERNAL_ID}, {IMG_URL_TO_DL, TYPE_TEXT},
                {NUMBER_ATTEMPT, TYPE_INT}, {"UNIQUE", "(" + ENTRY_ID + ", " + IMG_URL_TO_DL + ") ON CONFLICT IGNORE"}};

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/tasks");
    }
}

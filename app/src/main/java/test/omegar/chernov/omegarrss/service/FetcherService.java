/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package test.omegar.chernov.omegarrss.service;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import test.omegar.chernov.omegarrss.Constants;
import test.omegar.chernov.omegarrss.MainApplication;
import test.omegar.chernov.omegarrss.R;
import test.omegar.chernov.omegarrss.parser.RssAtomParser;
import test.omegar.chernov.omegarrss.provider.FeedData;
import test.omegar.chernov.omegarrss.provider.FeedData.TaskColumns;
import test.omegar.chernov.omegarrss.utils.NetworkUtils;
import test.omegar.chernov.omegarrss.utils.PrefUtils;


public class FetcherService extends IntentService {

    private final String LOG_TAG = RssAtomParser.class.getSimpleName();

    public static final String ACTION_REFRESH_FEEDS = "net.fred.feedex.REFRESH";
    public static final String ACTION_DOWNLOAD_IMAGES = "net.fred.feedex.DOWNLOAD_IMAGES";

    private static final int MAX_TASK_ATTEMPT = 3;

    private static final int FETCHMODE_DIRECT = 1;
    private static final int FETCHMODE_REENCODE = 2;

    private static final String CHARSET = "charset=";
    private static final String ENCODING = "encoding=\"";

    /* Allow different positions of the "rel" attribute w.r.t. the "href" attribute */
    private static final Pattern FEED_LINK_PATTERN = Pattern.compile(
            "[.]*<link[^>]* ((rel=alternate|rel=\"alternate\")[^>]* href=\"[^\"]*\"|href=\"[^\"]*\"[^>]* (rel=alternate|rel=\"alternate\"))[^>]*>",
            Pattern.CASE_INSENSITIVE);

    private final Handler mHandler;

    public FetcherService() {
        super(FetcherService.class.getSimpleName());
        HttpURLConnection.setFollowRedirects(true);
        mHandler = new Handler();
    }

    // Insert tasks to download images from internet and cache them in local file system
    public static void addImagesToDownload(ArrayList<Pair<String, String>> images) {
        if (images != null && !images.isEmpty()) {
            ContentValues[] values = new ContentValues[images.size()];
            for (int i = 0; i < images.size(); i++) {
                values[i] = new ContentValues();
                values[i].put(TaskColumns.ENTRY_ID, images.get(i).first);
                values[i].put(TaskColumns.IMG_URL_TO_DL, images.get(i).second);
            }

            MainApplication.getContext().getContentResolver().bulkInsert(TaskColumns.CONTENT_URI, values);
        }
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent == null) { // No intent, we quit
            return;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        // Connectivity issue, we quit
        if (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED) {
            if (ACTION_REFRESH_FEEDS.equals(intent.getAction())) {
                // Display a toast in that case
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(FetcherService.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return;
        }

        if (ACTION_DOWNLOAD_IMAGES.equals(intent.getAction())) {
            downloadAllImages();
        } else { // == Constants.ACTION_REFRESH_FEEDS
            PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, true);
            if(intent.getBooleanExtra(Constants.NEED_CLEANING, false)) {
                // Feed url changed
                // Remove all the entries and images
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.delete(FeedData.EntryColumns.CONTENT_URI, null, null);
                NetworkUtils.deleteEntriesImagesCache();
            }
            refreshFeed(PrefUtils.getFeedUrl());
            downloadAllImages();

            PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false);
        }
    }

    private void downloadAllImages() {
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        Cursor cursor = cr.query(   TaskColumns.CONTENT_URI, new String[]{TaskColumns._ID, TaskColumns.ENTRY_ID, TaskColumns.IMG_URL_TO_DL,
                                    TaskColumns.NUMBER_ATTEMPT}, TaskColumns.IMG_URL_TO_DL + Constants.DB_IS_NOT_NULL, null, null);

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        while (cursor.moveToNext()) {
            long taskId = cursor.getLong(0);
            long entryId = cursor.getLong(1);
            String imgPath = cursor.getString(2);
            int nbAttempt = 0;
            if (!cursor.isNull(3)) {
                nbAttempt = cursor.getInt(3);
            }

            try {
                NetworkUtils.downloadImage(entryId, imgPath);
                // If we are here, everything is OK
                operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
            } catch (Exception e) {
                if (nbAttempt + 1 > MAX_TASK_ATTEMPT) {
                    operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
                } else {
                    ContentValues values = new ContentValues();
                    values.put(TaskColumns.NUMBER_ATTEMPT, nbAttempt + 1);
                    operations.add(ContentProviderOperation.newUpdate(TaskColumns.CONTENT_URI(taskId)).withValues(values).build());
                }
            }
        }

        cursor.close();

        if (!operations.isEmpty()) {
            try {
                cr.applyBatch(FeedData.AUTHORITY, operations);
            } catch (Throwable ignored) {
            }
        }
    }

    private int refreshFeed(String feedUrl) {
        RssAtomParser handler = null;

        HttpURLConnection connection;
        int fetchMode = 0;

        try {
            connection = NetworkUtils.setupConnection(feedUrl);
            String contentType = connection.getContentType();

            long lastUpdMillis = PrefUtils.getLong(PrefUtils.LAST_UPDATE, -1);

            if(lastUpdMillis == -1) {   // If never updated this than take entries for the last 7 days
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -7);
                lastUpdMillis = cal.getTimeInMillis();
            }

            handler = new RssAtomParser(new Date(lastUpdMillis), feedUrl);

            if (contentType != null) {
                int index = contentType.indexOf(CHARSET);

                if (index > -1) {
                    int index2 = contentType.indexOf(';', index);

                    try {
                        Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8, index2) : contentType.substring(index + 8));
                        fetchMode = FETCHMODE_DIRECT;
                    } catch (UnsupportedEncodingException ignored) {
                        fetchMode = FETCHMODE_REENCODE;
                    }
                } else {
                    fetchMode = FETCHMODE_REENCODE;
                }

            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                char[] chars = new char[20];

                int length = bufferedReader.read(chars);

                String xmlDescription = new String(chars, 0, length);

                connection.disconnect();
                connection = NetworkUtils.setupConnection(connection.getURL());

                int start = xmlDescription.indexOf(ENCODING);

                if (start > -1) {
                    try {
                        Xml.findEncodingByName(xmlDescription.substring(start + 10, xmlDescription.indexOf('"', start + 11)));
                        fetchMode = FETCHMODE_DIRECT;
                    } catch (UnsupportedEncodingException ignored) {
                        fetchMode = FETCHMODE_REENCODE;
                    }
                } else {
                    // absolutely no encoding information found
                    fetchMode = FETCHMODE_DIRECT;
                }
            }

            switch (fetchMode) {
                default:
                case FETCHMODE_DIRECT: {
                    if (contentType != null) {
                        int index = contentType.indexOf(CHARSET);

                        int index2 = contentType.indexOf(';', index);

                        InputStream inputStream = connection.getInputStream();
                        Xml.parse(inputStream,
                                Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8, index2) : contentType.substring(index + 8)),
                                handler);
                    } else {
                        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                        Xml.parse(reader, handler);
                    }
                    break;
                }
                case FETCHMODE_REENCODE: {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    InputStream inputStream = connection.getInputStream();

                    byte[] byteBuffer = new byte[4096];

                    int n;
                    while ((n = inputStream.read(byteBuffer)) > 0) {
                        outputStream.write(byteBuffer, 0, n);
                    }

                    String xmlText = outputStream.toString();

                    int start = xmlText != null ? xmlText.indexOf(ENCODING) : -1;

                    if (start > -1) {
                        Xml.parse(
                                new StringReader(new String(outputStream.toByteArray(),
                                        xmlText.substring(start + 10, xmlText.indexOf('"', start + 11)))), handler
                        );
                    } else {
                        // use content type
                        if (contentType != null) {
                            int index = contentType.indexOf(CHARSET);

                            if (index > -1) {
                                int index2 = contentType.indexOf(';', index);

                                try {
                                    StringReader reader = new StringReader(new String(outputStream.toByteArray(), index2 > -1 ? contentType.substring(
                                            index + 8, index2) : contentType.substring(index + 8)));
                                    Xml.parse(reader, handler);
                                } catch (Exception ignored) {
                                }
                            } else {
                                StringReader reader = new StringReader(new String(outputStream.toByteArray()));
                                Xml.parse(reader, handler);
                            }
                        }
                    }
                    break;
                }
            }

            connection.disconnect();
        } catch (FileNotFoundException e) {
            if (handler == null || (!handler.isDone() && !handler.isCancelled())) {
                Log.e(LOG_TAG, e.getMessage());
                // No feed on this url
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(FetcherService.this, R.string.feed_not_found, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Throwable e) {
            if (handler == null || (!handler.isDone() && !handler.isCancelled())) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        return handler != null ? handler.getNewCount() : 0;
    }
}

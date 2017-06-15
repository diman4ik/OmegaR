package test.omegar.chernov.omegarrss;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import test.omegar.chernov.omegarrss.data.Entry;
import test.omegar.chernov.omegarrss.provider.FeedData;
import test.omegar.chernov.omegarrss.service.FetcherService;
import test.omegar.chernov.omegarrss.utils.PrefUtils;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String BUNDLE_RECYCLER_LAYOUT = "entrylist.recycler.layout";
    private static final int FEED_LOADER = 0;

    private EntriesRecyclerAdapter mEntriesAdapter;
    private RecyclerView mEntriesRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEntriesAdapter = new EntriesRecyclerAdapter(this);
        mEntriesRecyclerView = (RecyclerView)findViewById(R.id.entrylist_recycler_view);
        mEntriesRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager cardLayoutManager = new LinearLayoutManager(this);
        mEntriesRecyclerView.setLayoutManager(cardLayoutManager);
        mEntriesRecyclerView.setAdapter(mEntriesAdapter);

        //Set list item click to open EntryActivity to show entry details
        mEntriesRecyclerView.addOnItemTouchListener(new EntriesRecyclerAdapter.RecyclerTouchListener(this, mEntriesRecyclerView, new EntriesRecyclerAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Intent details = new Intent(MainActivity.this, EntryActivity.class);
                Entry entry = mEntriesAdapter.getItem(position);
                details.putExtra(EntryActivity.ENTRY_KEY, entry);
                startActivity(details);
            }

            @Override
            public void onLongClick(View view, int position) {}
        }));

        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipeRefreshLayout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                runRefreshTask(false);
            }
        });

        //http://rss.nytimes.com/services/xml/rss/nyt/Technology.xml
        //"http://www.sport-express.ru/services/materials/news/football/se/"
        //"http://www.economist.com/sections/economics/rss.xml"

        //PrefUtils.putString(PrefUtils.FEED_URL, "http://www.economist.com/sections/economics/rss.xml");
        //PrefUtils.putString(PrefUtils.FEED_URL, "http://www.sport-express.ru/services/materials/news/football/se/");
        //PrefUtils.putString(PrefUtils.FEED_URL, "http://rss.nytimes.com/services/xml/rss/nyt/Technology.xml");
        getSupportLoaderManager().initLoader(FEED_LOADER, null, this);

        if (savedInstanceState != null) {
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
            mEntriesRecyclerView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
        }

        PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, mEntriesRecyclerView.getLayoutManager().onSaveInstanceState());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void runRefreshTask(boolean needClean) {
        if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            getBaseContext().startService(  new Intent(getBaseContext(), FetcherService.class)
                                            .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                                            .putExtra(Constants.NEED_CLEANING, needClean));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Load all entries into cursor
        return new CursorLoader(this,
                FeedData.EntryColumns.CONTENT_URI,
                null,
                null,
                null,
                null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToFirst();
        mEntriesAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mEntriesAdapter.changeCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(PrefUtils.IS_REFRESHING.equals(key)) {
            if (PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                mSwipeRefreshLayout.setRefreshing(true);
            }
            else {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
        else if(getString(R.string.pref_feedurl_key).equals(key)) {
            // Feed url preference changed, reload feed
            PrefUtils.putLong(PrefUtils.LAST_UPDATE, -1);
            runRefreshTask(true);
        }
    }
}
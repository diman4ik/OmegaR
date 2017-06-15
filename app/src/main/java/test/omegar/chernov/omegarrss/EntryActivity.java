package test.omegar.chernov.omegarrss;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import test.omegar.chernov.omegarrss.data.Entry;
import test.omegar.chernov.omegarrss.utils.StringUtils;


public class EntryActivity extends AppCompatActivity {
    public static final String ENTRY_KEY = "entry_key";
    private Entry mEntry;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        mEntry = getIntent().getParcelableExtra(ENTRY_KEY);

        TextView titleView = (TextView)findViewById(R.id.entry_title);
        titleView.setText(mEntry.getTitle());

        TextView dateview = (TextView)findViewById(R.id.entry_date);
        String timeString = StringUtils.getDateTimeString(mEntry.getDate().getTime());
        dateview.setText(timeString);

        TextView authorView = (TextView)findViewById(R.id.entry_author);
        authorView.setText(mEntry.getAuthor());

        TextView contentView = (TextView)findViewById(R.id.entry_content);
        contentView.setText(mEntry.getAbstractText());

        ImageView contentImage = (ImageView)findViewById(R.id.entry_image);

        if(mEntry.getImageUrl() != null && !mEntry.getImageUrl().isEmpty()) {
            Glide.with(this).load(mEntry.getImageUrl()).fitCenter().into(contentImage);
        }
        else {
            contentImage.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    public void showInWebView(View v) {
        // Launch main link from entry
        Intent details = new Intent(this, WebActivity.class);
        details.putExtra(WebActivity.URL_KEY, mEntry.getLink());
        startActivity(details);
    }
}
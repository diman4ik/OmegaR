package test.omegar.chernov.omegarrss;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class WebActivity extends AppCompatActivity {
    public static final String URL_KEY = "url_key";
    private WebView mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        mView = (WebView)findViewById(R.id.webView);
        mView.setWebViewClient(new MyBrowser());

        final String url = getIntent().getStringExtra(URL_KEY);

        mView.getSettings().setLoadsImagesAutomatically(true);
        mView.getSettings().setJavaScriptEnabled(true);
        mView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mView.loadUrl(url);
    }

    // If you click on any link inside the webpage of the WebView, normally that page will not be loaded inside your WebView.
    // In order to do that you need to extend your class from WebViewClient and override its method.
    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
